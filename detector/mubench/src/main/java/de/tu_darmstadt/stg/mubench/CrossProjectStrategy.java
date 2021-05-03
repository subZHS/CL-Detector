package de.tu_darmstadt.stg.mubench;

import com.google.common.collect.Multiset;
import de.tu_darmstadt.stg.mubench.cli.DetectionStrategy;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tu_darmstadt.stg.mubench.cli.DetectorOutput;
import de.tu_darmstadt.stg.mubench.expand.PatternExpander;
import de.tu_darmstadt.stg.mudetect.*;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.dot.AUGDotExporter;
import de.tu_darmstadt.stg.mudetect.aug.model.dot.AUGEdgeAttributeProvider;
import de.tu_darmstadt.stg.mudetect.aug.model.dot.AUGNodeAttributeProvider;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.aug.persistence.AUGReader;
import de.tu_darmstadt.stg.mudetect.aug.persistence.AUGWriter;
import de.tu_darmstadt.stg.mudetect.aug.persistence.PersistenceAUGDotExporter;
import de.tu_darmstadt.stg.mudetect.aug.persistence.PersistenceAUGDotImporter;
import de.tu_darmstadt.stg.mudetect.aug.visitors.BaseAUGLabelProvider;
import de.tu_darmstadt.stg.mudetect.aug.visitors.WithSourceLineNumberLabelProvider;
import de.tu_darmstadt.stg.sourcerule.SourceRule;
import de.tu_darmstadt.stg.sourcerule.SourceRuleParser;
import de.tu_darmstadt.stg.yaml.YamlCollection;
import edu.iastate.cs.mudetect.mining.*;
import de.tu_darmstadt.stg.mudetect.model.Violation;
import de.tu_darmstadt.stg.mudetect.overlapsfinder.AlternativeMappingsOverlapsFinder;
import de.tu_darmstadt.stg.mudetect.ranking.*;
import de.tu_darmstadt.stg.mustudies.UsageUtils;
import de.tu_darmstadt.stg.yaml.YamlObject;
import edu.iastate.cs.egroum.aug.TypeUsageExamplePredicate;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import de.tu_darmstadt.stg.mubench.Misuse.ViolationType;

public class CrossProjectStrategy implements DetectionStrategy {

    public static String trainProjectBasePath = "/Users/hsz/zhs/sjtu/APIMisuse/dataset/Boa/projects/MuDetectProjects";
    public static Map<String, List<String>> specialAPIs = new HashMap<>();
    static {
        specialAPIs.put("java.io.FileInputStream", Arrays.asList("KyLinOS_android_packages_apps_Mms"));
        specialAPIs.put("java.lang.StringBuilder", Arrays.asList("iGio90_packages_providers_ContactsProvider"));
        specialAPIs.put("java.sql.ResultSet", Arrays.asList("KyLinOS_android_packages_apps_Mms"));
    }

    /**
     * 从训练项目中挖掘Pattern
     */
    public static Model minePatternPerAPI(API api, String[] dependencyClassPaths, DetectorOutput.Builder output) throws Exception{
        //TODO TypeUsageExamplePredicate
        TypeUsageExamplePredicate examplePredicate = TypeUsageExamplePredicate.usageExamplesOf(api.getName());
        String logPrefix = api.getSimpleName();

        File patternDir = Paths.get(getPatternBasePath().toString(), api.getName()).toFile();
        Model model;
        if(patternDir.exists() && patternDir.list()!=null && patternDir.list().length > 0){
            //如果pattern已存在，不需要挖掘，后续从持久化文件中读取出patterns
            return null;
        }
        patternDir.mkdirs();
        System.out.println(String.format("[MuDetectXProject] Target API = %s", api));
        Collection<APIUsageExample> trainingExamples = loadTrainingExamples(api, logPrefix, examplePredicate, dependencyClassPaths, output);
        if(output!=null) {
            output.withRunInfo(logPrefix + "-numberOfTrainingExamples", trainingExamples.size());
            //TODO (myCode) MuDetectStrategy.getTypeUsageCounts
            output.withRunInfo(logPrefix + "-numberOfUsagesInTrainingExamples", MuDetectStrategy.getTypeUsageCounts(trainingExamples));
        }
        model = createMiner().mine(trainingExamples);
        //pattern按照support排序
        List<APIUsagePattern> patternSortedListPerAPI = sortPatterns(model.getPatterns(), new HashSet<>(Arrays.asList(api.getSimpleName())));
        System.out.println(logPrefix + "-original patterns size: " + model.getPatterns().size());
        model = new MinPatternActionsModel(() -> new LinkedHashSet<>(patternSortedListPerAPI), 1);
        System.out.println(logPrefix + "-patterns size after pruning: " + model.getPatterns().size());
        //持久化patterns
        reportPattern(model.getPatterns(), patternDir.getAbsolutePath(), "patterns");
        return model;
    }

    public Set<String> involveAPIs(List<TargetProject> targetProjectList) throws Exception {
        Set<String> minedForAPIs = new HashSet<>();
        for(TargetProject targetProject : targetProjectList) {
            for (Misuse misuse : targetProject.getMisuses()) {
                List<API> apis = misuse.getMisusedAPI();
                for (API api : apis) {
                    if (minedForAPIs.contains(api.getName()))
                        continue;

                    //TODO (myCode)判断api是否在范围内
                    if (api.fulfillLibAttr() == null) {
                        continue;
                    }
                    minedForAPIs.add(api.getName());
                }
            }
        }
        return minedForAPIs;
    }

    @Override
    public DetectorOutput detectViolations(DetectorArgs args, DetectorOutput.Builder output) throws Exception {
        //TODO (myCode)
        List<TargetProject> targetProjectList = TargetProject.find(getIndexFilePath(), args.getTargetSrcPaths());
        Collection<APIUsageExample> targets = loadDetectionTargets(args, targetProjectList);
        output.withRunInfo("numberOfTargets", targets.size());

        Set<APIUsagePattern> patterns = new HashSet<>();
        Set<String> minedForAPIs = involveAPIs(targetProjectList);
        Set<String> minedForAPISimpleNames = new HashSet<>();
        for(String apiName : minedForAPIs){
            API api = new API(apiName);
            minedForAPISimpleNames.add(api.getSimpleName());
            String logPrefix = api.getSimpleName();
            try {
                Model model = minePatternPerAPI(api, args.getDependencyClassPath(), output);
                if (model == null) {//model为null表示，文件夹存在，从持久化文件中读取出patterns
                    File patternDir = Paths.get(getPatternBasePath().toString(), api.getName()).toFile();
                    List<APIUsagePattern> patternSortedListPerAPI = restorePatterns(patternDir.getAbsolutePath(), "patterns");
                    model = () -> new LinkedHashSet<>(patternSortedListPerAPI);
                    System.out.println(logPrefix + "-patterns size read from file: " + model.getPatterns().size());
                }

                output.withRunInfo(logPrefix + "-numberOfPatterns", model.getPatterns().size());
                output.withRunInfo(logPrefix + "-maxPatternSupport", model.getMaxPatternSupport());
                patterns.addAll(model.getPatterns());
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        }

        //pattern按照support排序
        List<APIUsagePattern> patternSortedList = sortPatterns(patterns, minedForAPISimpleNames);
        Model model = () -> new LinkedHashSet<>(patternSortedList);
        System.out.println(String.format("Using %d APIs: %s, final all patterns size: %d", minedForAPIs.size(), minedForAPIs.toString(), model.getPatterns().size()));

        List<Violation> violations = createDetector(model).findViolations(targets, null);

        File resultDir = MuDetectRunner.getResultDir(args.getTargetSrcPaths()[0], true);
        reportPattern(model.getPatterns(), resultDir.getAbsolutePath(), "patterns");

//        //获取SourceCode Rules
//        System.out.printf("parsing sourceRules of API %s: \n", String.join("| ", minedForAPIs));
//        List<SourceRule> sourceRuleList = SourceRuleParser.obtainSourceRules(minedForAPIs);
//        System.out.println("original sourceRules size: " + sourceRuleList.size());
//        int ruleSupport = patternSortedList.size()>0 ? patternSortedList.get(0).getSupport() : 1;
//        List<APIUsagePattern> rulePatternList = PatternExpander.ruleToPatterns(sourceRuleList, ruleSupport);
//        //SourceCode Rules和pattern融合得到新pattern
//        List<APIUsagePattern> newPatternList = PatternExpander.expandNewPattern(patternSortedList, sourceRuleList);
//        newPatternList.addAll(rulePatternList);
//        newPatternList.addAll(model.getPatterns());
//        model = new MinPatternActionsModel(() -> new LinkedHashSet<>(newPatternList), 1);//TODO (myCode) 2改成1
//        System.out.println("patterns & rules size: " + model.getPatterns().size());
//
//        reportPattern(model.getPatterns(), resultDir.getAbsolutePath(), "extendedPatterns");
//
//        List<Violation> violations = createDetector(model).findViolations(targets, sourceRuleList);

        Set<APIUsageExample> misuses = violations.stream().map(violation -> violation.getOverlap().getTarget()).collect(Collectors.toSet());

        output.withRunInfo("numberOfViolations", violations.size());
        output.withRunInfo("numberOfMisuses", misuses.size());
        output.withRunInfo("Misuses", misuses.stream().map(example -> example.getLocation().getFilePath() + "#" + example.getLocation().getMethodSignature()).collect(Collectors.toList()));
        System.out.println("violations num: " + violations.size());
        System.out.println("Misuses num: " + misuses.size());
        output.withRunInfo("numberOfExploredAlternatives", AlternativeMappingsOverlapsFinder.numberOfExploredAlternatives);

        //得到ground-truth误用
        Collection<Misuse> groundTruthMisuses = targetProjectList.stream().map(TargetProject::getMisuses)
                .reduce(new HashSet<>(), (a, b) -> {a.addAll(b);return a;});
        output.withRunInfo("groundTruthMisuseNum", groundTruthMisuses.size());
        Set<Misuse> foundGroundTruthMisuses = extractGroundTruthMisuses(targetProjectList, misuses);
        output.withRunInfo("foundGroundTruthMisuseNum", foundGroundTruthMisuses.size());
        output.withRunInfo("foundGroundTruthMisuses", foundGroundTruthMisuses.stream().map(misuse -> misuse.getFilePath() + "#" + misuse.getMethodSignature()).collect(Collectors.toList()));

        //计算召回误用的各类型个数
        Map<ViolationType, Integer> violationTypeNumMap = calcurateMisuseNumForEachType(groundTruthMisuses);
        output.withRunInfo("groundTruthMisuseTypeDistribution", violationTypeNumMap.keySet().stream().map(type-> type.name()+" "+violationTypeNumMap.get(type)).collect(Collectors.toList()));
        Map<ViolationType, Integer> foundViolationTypeNumMap = calcurateMisuseNumForEachType(foundGroundTruthMisuses);
        output.withRunInfo("FoundGroundTruthMisuseTypeDistribution", foundViolationTypeNumMap.keySet().stream().map(type-> type.name()+" "+foundViolationTypeNumMap.get(type)).collect(Collectors.toList()));

        return output.withFindings(violations, ViolationUtils::toFinding);
    }

    private static Set<Misuse> extractGroundTruthMisuses(List<TargetProject> targetProjectList, Set<APIUsageExample> misuseExamples){
        Set<String> misuseLocationList = new HashSet<>();
        if(misuseExamples!=null){
            for(APIUsageExample example : misuseExamples){
                misuseLocationList.add(example.getLocation().getFilePath()+"#"+example.getLocation().getMethodSignature());
            }
        }

        Set<Misuse> misuses = new HashSet<>();
        for(TargetProject project : targetProjectList){
            for(Misuse misuse : project.getMisuses()){
                if(anyContains(misuseLocationList, misuse.getFilePath()) && anyContains(misuseLocationList, misuse.getMethodSignature())){
                    misuses.add(misuse);
                }
            }
        }
        return misuses;
    }

    private static boolean anyContains(Collection<String> strings, String substring){
        for (String string : strings) {
            if (string.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    public static Map<ViolationType, Integer> calcurateMisuseNumForEachType(Collection<Misuse> misuses){
        Map<ViolationType, Integer> violationTypeNumMap = new HashMap<>();
        for(Misuse misuse : misuses){
            for(ViolationType type : misuse.getViolationTypes()){
                violationTypeNumMap.putIfAbsent(type, 0);
                violationTypeNumMap.put(type, violationTypeNumMap.get(type)+1);
                break;
            }
        }
        return violationTypeNumMap;
    }

    private static List<APIUsagePattern> sortPatterns(Collection<APIUsagePattern> patterns, Set<String> APISimpleNames){
        return patterns.stream()
                .sorted(Comparator.comparing(p -> ((APIUsagePattern)p).containAPINodes(APISimpleNames)? 1:0).reversed()
                        .thenComparing(Comparator.comparing(p->((APIUsagePattern)p).getSupport()).reversed())
                        .thenComparing(Comparator.comparing(p -> ((APIUsagePattern)p).vertexSet().size()).reversed())
                ).collect(Collectors.toList());
    }

    private static Collection<APIUsageExample> loadTrainingExamples(API targetType, String logPrefix, TypeUsageExamplePredicate examplePredicate, String[] dependencyClassPaths, DetectorOutput.Builder output) {
        int maxExampleSize = 1000;
        List<ExampleProject> exampleProjects = getExampleProjects(targetType);
        System.out.println(String.format("[MuDetectXProject] Example Projects = %d", exampleProjects.size()));
        if(output != null) {
            output.withRunInfo(logPrefix + "-exampleProjects", exampleProjects.size());
        }

        AUGBuilder builder = new AUGBuilder(new DefaultAUGConfiguration() {{
            usageExamplePredicate = examplePredicate;
        }});
        List<APIUsageExample> targetTypeExamples = new ArrayList<>();
        int trainProjectIndex = 0;
        for (ExampleProject exampleProject : exampleProjects) {
            String projectName = exampleProject.getProjectPath();
            if(specialAPIs.get(targetType.getName())!=null){
                boolean isIgnoreProject = false;
                for(String ignoreName : specialAPIs.get(targetType.getName())){
                    if(projectName.contains(ignoreName)){
                        isIgnoreProject = true;
                        System.out.println("specialAPI " + targetType.getName() + ", ignoreName " + ignoreName + " come out, prjectName = " + projectName);
                        break;
                    }
                }
                if(isIgnoreProject)
                    continue;
            }
            List<APIUsageExample> projectExamples = new ArrayList<>();
            for (String srcDir : exampleProject.getSrcDirs()) {
                Path projectSrcPath = Paths.get(exampleProject.getProjectPath(), srcDir);
                System.out.println(String.format("[MuDetectXProject] trainProjectIndex=%d, Scanning path %s", trainProjectIndex, projectSrcPath));
                PrintStream originalSysOut = System.out;
                try {
                    System.setOut(new PrintStream(new OutputStream() {
                        @Override
                        public void write(int arg0) {}
                    }));
                    projectExamples.addAll(builder.build(projectSrcPath.toString(), dependencyClassPaths));
                } catch (Exception e) {
                    System.err.print("[MuDetectXProject] Parsing failed: ");
                    e.printStackTrace(System.err);
                } finally {
                    System.setOut(originalSysOut);
                }
            }
            System.out.println(String.format("[MuDetectXProject] Examples from Project %s = %d", exampleProject.getProjectPath(), projectExamples.size()));
            int maxNumberOfExamplesPerProject = maxExampleSize / exampleProjects.size();
            if(projectExamples.size() > maxNumberOfExamplesPerProject){
                projectExamples = pickNRandomElements(projectExamples, maxNumberOfExamplesPerProject, new Random(projectName.hashCode()));
                System.out.println(String.format("[MuDetectXProject] Too many examples, sampling %d.", maxNumberOfExamplesPerProject));
            }

            targetTypeExamples.addAll(projectExamples);
            trainProjectIndex++;
        }
        //TODO (myCode) change cross_project_training_examples size
//        System.out.println(String.format("[MuDetectXProject] Examples from All training Project = %d", targetTypeExamples.size()));
//        if (targetTypeExamples.size() > maxExampleSize) {
//            targetTypeExamples = pickNRandomElements(targetTypeExamples, maxExampleSize, new Random());
//            System.out.println(String.format("[MuDetectXProject] Too many examples, sampling %d.", maxExampleSize));
//        }

        System.out.println(String.format("[MuDetectXProject] Examples = %d", targetTypeExamples.size()));

        return targetTypeExamples;
    }

    private static List<ExampleProject> getExampleProjects(API targetType) {
        Path dataFile = Paths.get(getTrainProjectYmlBasePath().toString(), targetType + ".yml");
        try (InputStream is = new FileInputStream(dataFile.toFile())) {
            return StreamSupport.stream(new Yaml().loadAll(is).spliterator(), false)
                    .map(ExampleProject::create).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to load example data for " + targetType, e);
        }
    }

    static class ExampleProject {
        private final String projectPath;
        private final List<String> srcDirs;

        ExampleProject(String projectPath, List<String> srcDirs) {
            this.projectPath = projectPath;
            this.srcDirs = srcDirs;
        }

        static ExampleProject create(Object yamlSpec) {
            Map<String, Object> data = (Map<String, Object>) yamlSpec;
            String projectPath = Paths.get(trainProjectBasePath, (String) data.get("path")).toString();
            if(!new File(projectPath).exists()){
                throw new RuntimeException("projectPath: " + projectPath + " not exist.");
            }
            List<String> srcDirs = (List<String>) data.get("source_paths");
            return new ExampleProject(projectPath, srcDirs);
        }

        String getProjectPath() {
            return projectPath;
        }

        List<String> getSrcDirs() {
            return srcDirs;
        }
    }

    public static Path getIndexFilePath() throws FileNotFoundException {
        Path path = Paths.get(getExamplesBasePath().toString(), "targetProjects.csv");
        if (!Files.exists(path)) {
            throw new FileNotFoundException("No index file '" + path + "'.");
        }
        return path;
    }

    public static Path getExamplesBasePath() {
        return Paths.get(getMuBenchBasePath().toString(), "checkouts-xp");
    }

    public static Path getMuBenchBasePath() {
        return Paths.get(".");
    }

    public static Path getTrainProjectYmlBasePath(){
        return Paths.get(getExamplesBasePath().toString(), "trainProjectsPerAPI");
    }

    public static Path getSourceRuleFilePath(){
        return Paths.get(getExamplesBasePath().toString(), "rule.csv");
    }

    public static Path getPatternBasePath() {
        return Paths.get(getMuBenchBasePath().toString(), "patternResults");
    }

    private static <E> List<E> pickNRandomElements(List<E> list, int n, Random r) {
        int length = list.size();
        if (length < n) return list;

        for (int i = length - 1; i >= length - n; --i) {
            Collections.swap(list, i , r.nextInt(i + 1));
        }
        return list.subList(length - n, length);
    }

    private YamlObject getTypeUsageCounts(Collection<APIUsageExample> targets) {
        YamlObject object = new YamlObject();
        for (Multiset.Entry<String> entry : UsageUtils.countNumberOfUsagesPerType(targets).entrySet()) {
            object.put(entry.getElement(), entry.getCount());
        }
        return object;
    }

    private static AUGMiner createMiner() {
        return new DefaultAUGMiner(new DefaultMiningConfiguration() {{
            occurenceLevel = Level.CROSS_PROJECT;
            minPatternSupport = 5;
        }});
    }

    public Collection<APIUsageExample> loadDetectionTargets(DetectorArgs args, List<TargetProject> targetProjectList) throws Exception {
        //TODO (myCode)
        Collection<APIUsageExample> targetMisuseExamples = new ArrayList<>();
        for(TargetProject targetProject: targetProjectList){
//            for(String targetSrcPath : args.getTargetSrcPaths()){
//                if(targetSrcPath.contains(targetProject.getProjectId())) {
//                    String targetProjectPath = Paths.get(targetSrcPath.split(targetProject.getProjectId())[0], targetProject.getProjectId()).toString();
//                    GitUtil.checkout(targetProjectPath, targetProject.getVersionId());
//                    break;
//                }
//            }

            AUGBuilder builder = new AUGBuilder(new DefaultAUGConfiguration() {{
                usageExamplePredicate = MisuseInstancePredicate.examplesOf(targetProject.getMisuses());
            }});
            targetMisuseExamples.addAll(builder.build(args.getTargetSrcPaths(), args.getDependencyClassPath()));
        }
        return targetMisuseExamples;
    }

    private MuDetect createDetector(Model model) {
        return new MuDetect(
                model,
                new AlternativeMappingsOverlapsFinder(new DefaultOverlapFinderConfig(new DefaultMiningConfiguration())),
                new FirstDecisionViolationPredicate(
                        new MissingDefPrefixNoViolationPredicate(),
                        new OnlyDefPrefixNoViolationPredicate(),
//                        new MissingCatchNoViolationPredicate(),
                        new MissingAssignmentNoViolationPredicate(),
                        new MissingNullCheckLiteralNoViolationPredicate(),//TODO (myCode)
                        new IncorrectOrderViolationPredicate(),//TODO (myCode)
                        new MissingElementViolationPredicate()),
                new DefaultFilterAndRankingStrategy(
                        new WeightRankingStrategy(
                                    new ProductWeightFunction(
                                            new OverlapWithoutEdgesToMissingNodesWeightFunction(new ConstantNodeWeightFunction()),
                                            new PatternSupportWeightFunction(),
                                            new ViolationSupportWeightFunction()
                                    ))));
    }

    //TODO (myCode)
    private static void reportPattern(Collection<APIUsagePattern> patterns, String dirPath, String fileName) throws IOException {
        //savePatterns
        YamlCollection patternInfo = new YamlCollection();
        List<YamlObject> patternYamlObjectList = new ArrayList<>();

        AUGDotExporter dotExporter = new AUGDotExporter(new WithSourceLineNumberLabelProvider(new BaseAUGLabelProvider()), new AUGNodeAttributeProvider(), new AUGEdgeAttributeProvider());
        int index = 0;
        for(APIUsagePattern pattern: patterns){
            YamlObject o = new YamlObject();
            if(pattern.getRuleId()!=null) {
                o.put("ruleId", pattern.getRuleId());
                o.put("api", pattern.getRuleApi());
            }
            o.put("index", index);
            o.put("pattern", dotExporter.toDotGraph(pattern));
            o.put("support", pattern.getSupport());
            o.put("exampleLocations", ViolationUtils.getPatternInstanceLocations(pattern));
            patternYamlObjectList.add(o);
            index++;
        }
        patternInfo.appendDocuments(patternYamlObjectList);
        patternInfo.write(Files.newOutputStream(new File(dirPath + "/"+ fileName + ".yml").toPath()));

        //saveAUGs
        FileOutputStream out = new FileOutputStream(dirPath + "/" + fileName + ".zip");
        try (AUGWriter writer = new AUGWriter(out, new PersistenceAUGDotExporter())) {
            int i = 0;
            for(APIUsagePattern pattern : patterns){
                writer.write(pattern, "aug-" + i);
                i++;
            }
        }
    }

    //TODO (myCode)
    public static List<APIUsagePattern> restorePatterns(String dirPath, String fileName) throws Exception{
        List<APIUsagePattern> patternList = new ArrayList<>();
        //restoreAUGs
        FileInputStream in = new FileInputStream(dirPath + "/" + fileName + ".zip");
        try (AUGReader<APIUsagePattern> reader = new AUGReader<>(in, new PersistenceAUGDotImporter(), APIUsagePattern::new)) {
            patternList = new ArrayList<>(reader.readAll());
        }

        //restorePatterns
        Yaml yaml = new Yaml();
        Iterable<Object> iterator = yaml.loadAll(new FileInputStream(dirPath + "/" + fileName + ".yml"));
        int i=0;
        for(Object object : iterator){
            Map<String, Object> o = (Map<String, Object>)object;
            patternList.get(i).setSupport(Integer.valueOf(o.get("support").toString()));
            if(o.get("ruleId")!=null) {
                patternList.get(i).setRuleId(o.get("ruleId").toString());
                patternList.get(i).setRuleApi(o.get("api").toString());
            }
            List<String> locations = (List<String>)o.get("exampleLocations");
            patternList.get(i).setExampleLocations(locations.stream().map(l->ViolationUtils.parseLocation(l)).collect(Collectors.toSet()));
            i++;
        }
        return patternList;
    }

}
