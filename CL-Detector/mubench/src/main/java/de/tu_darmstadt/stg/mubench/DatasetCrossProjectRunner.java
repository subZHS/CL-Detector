package de.tu_darmstadt.stg.mubench;

import de.tu_darmstadt.stg.mubench.dataset.RecallRate;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.model.Violation;
import de.tu_darmstadt.stg.sourcerule.SourceRuleParser;
import edu.iastate.cs.mudetect.mining.Model;
import org.apache.commons.collections4.CollectionUtils;
import org.yaml.snakeyaml.Yaml;
import de.tu_darmstadt.stg.mubench.Misuse.ViolationType;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatasetCrossProjectRunner {

    public static void main(String[] args) throws Exception {
        onlyMinePatternOfAllAPIs();
        runDatasetMUDetectXP();
        calcurateRecall(Paths.get("results").toString());
    }

    private static void onlyMinePatternOfAllAPIs() throws Exception{
        File trainProjectYmlBaseDir = CrossProjectStrategy.getTrainProjectYmlBasePath().toFile();
        Set<String> apiList = allExistAPI();
        System.out.println("[MinePatternOfAllAPIs] Total API num : " + apiList.size());
        int index= 0;
        for(String apiClass : apiList){
            System.out.println(String.format("[MinedAPI Index %d]Current API : %s", index, apiClass));
            API api = new API(apiClass);
            String libDirName = api.getLibDirName();
            String apiLibDependencyPath = Paths.get(SourceRuleParser.libRootDir, libDirName, "jar").toString();
            Model model = CrossProjectStrategy.minePatternPerAPI(api, new String[]{apiLibDependencyPath}, null);
            if(model==null){
                System.out.println(String.format("patterns of API %s already mined.", apiClass));
            }
            index++;
        }
    }

    private static void runPrecisionOnTargetProjects() throws Exception {
        List<TargetProject> targetProjects = new ArrayList<>();
        targetProjects.add(new TargetProject("closure", "43c245f0ff8d409e81e25687e69d34666b7cf26a~1"));
        targetProjects.add(new TargetProject("itext", "adda8eb38978eeea6e86aca64ae746754c103dae"));
        targetProjects.add(new TargetProject("jodatime", "4f0fa2ece24061b8e47e793ada1cb1b6804df334~1"));
        targetProjects.add(new TargetProject("lucene", "0cb96adf12063c1ec2d586d0cec6a209abe9a2dd~1"));
        System.out.println("[runPrecisionOnTargetProjects] Total TargetProject num : " + targetProjects.size());

        List<String[]> argsList = new ArrayList<>();
        int index= 0;

        StringBuilder depLibClasspathBuilder = new StringBuilder();
        for(String apiName : allExistAPI()) {
            API api = new API(apiName);
            String libDirName = api.getLibDirName();
            if (depLibClasspathBuilder.length() != 0) {
                depLibClasspathBuilder.append(":");
            }
            depLibClasspathBuilder.append(Paths.get(SourceRuleParser.libRootDir, libDirName, "jar").toString());
        }

        for(TargetProject targetProject : targetProjects){
            String targetProjectPath = targetProject.getCodePath()[0];
            File resultDir = MuDetectRunner.getResultDir(MuDetectRunner.precisionResultDirName, targetProjectPath, false);
            if(resultDir.exists() && resultDir.list()!=null && resultDir.list().length > 0){
                System.out.println(String.format("skip TargetProject : %s", targetProject.getProjectId()+"/"+targetProject.getVersionId()));
                continue;
            }
            System.out.println(String.format("[TargetProject Index %d] Current TargetProject : %s", index, targetProject.getProjectId()+"/"+targetProject.getVersionId()));

            List<String> argList = new ArrayList<>();
            argList.add("is_precision_exp");
            argList.add("true");
            argList.add("detector_mode");
            argList.add("1");
            argList.add("target_src_path");
            argList.add(targetProjectPath);
            argList.add("dep_classpath");
            argList.add(depLibClasspathBuilder.toString());
            argsList.add(argList.toArray(new String[0]));
            index++;
        }
        System.out.printf("[runDatasetMUDetectXP] Need To Run TargetProject num : %d\n", index);

        int i=0;
        for(String[] argArray : argsList){
            System.out.println("[index " + i + "]execute args: " + String.join(" ", argArray));
            MuDetectCrossProjectRunner.main(argArray);
            i++;
        }
    }

    private static void runDatasetMUDetectXP() throws Exception{
        List<TargetProject> targetProjects = getAvailableTargetProjects();
        System.out.println("[runDatasetMUDetectXP] Total TargetProject num : " + targetProjects.size());
        List<String[]> argsList = new ArrayList<>();
        int index= 0;
        int skipIndex=0;
        for(TargetProject targetProject : targetProjects){
            String targetProjectPath = targetProject.getCodePath()[0];
            File resultDir = MuDetectRunner.getResultDir(targetProjectPath, false);
            if(resultDir.exists() && resultDir.list()!=null && resultDir.list().length > 0){
                System.out.println(String.format("skip %d TargetProject : %s", skipIndex, targetProject.getProjectId()+"/"+targetProject.getVersionId()));
                skipIndex++;
                continue;
            }
            System.out.println(String.format("[TargetProject Index %d] Current TargetProject : %s", index, targetProject.getProjectId()+"/"+targetProject.getVersionId()));
            StringBuilder depLibClasspathBuilder = new StringBuilder();
            for(Misuse misuse : targetProject.getMisuses()){
                for(API api : misuse.getMisusedAPI()) {
                    String libDirName = api.getLibDirName();
                    if (depLibClasspathBuilder.length() != 0) {
                        depLibClasspathBuilder.append(":");
                    }
                    depLibClasspathBuilder.append(Paths.get(SourceRuleParser.libRootDir, libDirName, "jar").toString());
                }
            }

            List<String> argList = new ArrayList<>();
            argList.add("detector_mode");
            argList.add("0");
            argList.add("target_src_path");
            argList.add(targetProjectPath);
            argList.add("dep_classpath");
            argList.add(depLibClasspathBuilder.toString());
            argsList.add(argList.toArray(new String[0]));
            index++;
        }
        System.out.printf("[runDatasetMUDetectXP] Need To Run TargetProject num : %d, skip Num: %d\n", index, skipIndex);

        int i=0;
        for(String[] argArray : argsList){
            System.out.println("[index " + i + "]execute args: " + String.join(" ", argArray));
            MuDetectCrossProjectRunner.main(argArray);
            i++;
        }
    }

    //目标项目存在，而且目标项目中存在可用的误用，当误用对应的api不在目标api范围内，误用不可用
    public static List<TargetProject> getAvailableTargetProjects() throws Exception{
        Path targetProjectCSVPath = CrossProjectStrategy.getIndexFilePath();
        List<TargetProject> targetProjects = TargetProject.find(targetProjectCSVPath, null);

        //过滤出路径存在的项目，而且有api的，其他的直接在原列表中删除
        for(TargetProject targetProject : new ArrayList<>(targetProjects)) {
            String targetProjectPath = targetProject.getCodePath()[0];
            if (!new File(targetProjectPath).exists()) {
                targetProjects.remove(targetProject);
                continue;
            }
            int count = 0;
            for (Misuse misuse : new ArrayList<>(targetProject.getMisuses())) {
                List<API> apis = misuse.getMisusedAPI();
                boolean existAPI = false;
                for(API api : apis) {
                    if (api.fulfillLibAttr() != null){
                        existAPI = true;
                        break;
                    }
                }
                if(!existAPI){
                    targetProject.getMisuses().remove(misuse);
                    continue;
                }
                misuse.setApi(apis);
                count++;
            }
            if (count == 0) {
                targetProjects.remove(targetProject);
            }
        }
        return targetProjects;
    }

    private static RecallRate calcurateRecall(String resultDirPath) throws Exception{
        List<TargetProject> targetProjects = getAvailableTargetProjects();
        //根据result文件夹进行过滤
        for(TargetProject targetProject : new ArrayList<>(targetProjects)) {
            File projectVersionDir = Paths.get(resultDirPath, targetProject.getProjectId(), targetProject.getVersionId()).toFile();
            if(!projectVersionDir.exists()){
                targetProjects.remove(targetProject);
            }
        }

        //计算被识别出的Misuse个数
        List<TargetProject> misuseFoundProjects = new ArrayList<>();
        for(TargetProject targetProject : targetProjects) {
            TargetProject findingProject = new TargetProject(targetProject);
            Set<String> misuseIdList = new HashSet<>();
            File findOutYml = Paths.get(resultDirPath, targetProject.getProjectId(), targetProject.getVersionId(), "findings-output.yml").toFile();
            if(!findOutYml.exists()){
                continue;
            }
            Iterable<Object> iterator = new Yaml().loadAll(new FileInputStream(findOutYml));
            for(Object object : iterator) {
                Map<String, String> map = (Map<String, String>) object;
                String file = map.get("file");
                String method = map.get("method");
                for(Misuse misuse : targetProject.getMisuses()){
                    if(file.contains(misuse.getFilePath()) && method.equals(misuse.getMethodSignature())){
                        if(!misuseIdList.contains(misuse.getId())){
                            findingProject.getMisuses().add(misuse);
                            misuseIdList.add(misuse.getId());
                        }
                        break;
                    }
                }
            }
            misuseFoundProjects.add(findingProject);
        }

        //统计召回率
        RecallRate recallRate = new RecallRate(misuseFoundProjects, targetProjects);

        //把找到的misuse标记写到文件中
        String MuBenchRecallResultCsvPath = Paths.get(CrossProjectStrategy.getExamplesBasePath().toString(), "MuBenchRecallResult.csv").toString();
        recallRate.printAllMisusesTaggedIfFound(MuBenchRecallResultCsvPath);

        System.out.println(String.format("recallRate: %s = %.2f %% in %d projects and %d apis.", recallRate.getFormula(), recallRate.getRecallRate() *100, recallRate.getProjectNum(), recallRate.getAPINum()));

        //计算召回误用的类型分布
        Map<ViolationType, Integer> foundViolationTypeNumMap = CrossProjectStrategy.calcurateMisuseNumForEachType(recallRate.getFoundMisuses());
        System.out.println("foundViolationTypeDistribution: " + foundViolationTypeNumMap);
        Map<ViolationType, Integer> groundTruthviolationTypeNumMap = CrossProjectStrategy.calcurateMisuseNumForEachType(recallRate.getTotalMisuses());
        System.out.println("groundTruthViolationTypeDistribution: " + groundTruthviolationTypeNumMap);

        System.out.println("recallRate per API:");
        List<Map.Entry<String, RecallRate>> apiRecallEntryList = recallRate.getAPIRecallMap();
        for(Map.Entry<String, RecallRate> entry : apiRecallEntryList){
            System.out.println(String.format("API %s: %s", entry.getKey(), entry.getValue().getFormula()));
        }

        System.out.println("recallRate per Project:");
        List<Map.Entry<String, RecallRate>> projectRecallEntryList = recallRate.getProjectRecallMap();
        for(Map.Entry<String, RecallRate> entry : projectRecallEntryList){
            System.out.println(String.format("Project %s: %s", entry.getKey(), entry.getValue().getFormula()));
        }

        return recallRate;
    }

    private static Set<String> allExistProjectId(){
        Set<String> existProjectId = new LinkedHashSet<>();
        for(File project : new File(TargetProject.targetProjectBasePath).listFiles()){
            if(project.getName().startsWith("."))
                continue;
            existProjectId.add(project.getName());
        }
        return existProjectId;
    }

    private static Set<String> allExistAPI(){
        Set<String> existAPI = new HashSet<>();
        File trainProjectYmlBaseDir = CrossProjectStrategy.getTrainProjectYmlBasePath().toFile();
        for(File trainProjectYml : trainProjectYmlBaseDir.listFiles()){
            existAPI.add(trainProjectYml.getName().replace(".yml", ""));
        }
        return existAPI;
    }


    private static Integer calcuratePatternNum(String patternBaseDirName) throws Exception{
        File patternBaseDir = new File(patternBaseDirName);
        int count = 0;
        for(File apiDir : patternBaseDir.listFiles()){
            if(apiDir.isDirectory() && apiDir.list()!=null && apiDir.list().length > 0){
                File patternYml = Paths.get(apiDir.getAbsolutePath(), "patterns.yml").toFile();
                if(!patternYml.exists()) continue;
                List<APIUsagePattern> patternSortedListPerAPI = CrossProjectStrategy.restorePatterns(apiDir.getAbsolutePath(), "patterns");
                count += patternSortedListPerAPI.size();
                System.out.printf("patterns num in API %s: %d\n", apiDir.getName(), patternSortedListPerAPI.size());
            }
        }
        System.out.println("patterns total num: " + count);
        return count;
    }

    private static Map<ViolationType, Integer> calcurateMisuseNumForEachType(String[] targetSrcPaths) throws Exception{
        //过滤不可用项目
        List<TargetProject> targetProjectList;
        if(targetSrcPaths==null) {
            targetProjectList = getAvailableTargetProjects();
        }else {
            targetProjectList = TargetProject.find(CrossProjectStrategy.getIndexFilePath(), targetSrcPaths);
        }

        Set<Misuse> misuseList = new HashSet<>();
        for(TargetProject project : targetProjectList){
            misuseList.addAll(project.getMisuses());
        }

        Map<ViolationType, Integer> violationTypeNumMap = CrossProjectStrategy.calcurateMisuseNumForEachType(misuseList);

        System.out.println("total misuses num in violationTypeNumMap:" + misuseList.size());
        System.out.println(violationTypeNumMap);
        return violationTypeNumMap;
    }
}
