package de.tu_darmstadt.stg.mubench;

import de.tu_darmstadt.stg.yaml.YamlCollection;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DatasetDiffResult {

    private Map<String, Map<String, TargetProject>> targetProjectMap;

    public static void main(String[] args) throws Exception {
        String resultDirWithoutRule = "/Users/hsz/zhs/sjtu/APIMisuse/dataset/MUDetectResults/resultsOnlyRule_12538";
        String resultDirWithRule = "/Users/hsz/zhs/sjtu/APIMisuse/dataset/MUDetectResults/resultsWithRule_12538";
        new DatasetDiffResult().findDiff(resultDirWithoutRule, resultDirWithRule);
    }

    private void findDiff(String resultDirWithoutRule, String resultDirWithRule) throws Exception {
        List<TargetProject> targetProjects = DatasetCrossProjectRunner.getAvailableTargetProjects();
        targetProjectMap = targetProjects.stream().collect(Collectors.groupingBy(TargetProject::getProjectId, Collectors.toMap(TargetProject::getVersionId, k->k)));

        Map<String, Map<String, Set<Finding>>> findingWithoutRule = loadFinding(resultDirWithoutRule);
        Map<String, Map<String, Set<Finding>>> findingWithRule = loadFinding(resultDirWithRule);

        Map<String, Map<String, Set<Finding>>> onlyInWithoutRule = findOnlyInOneMap(findingWithoutRule, findingWithRule);
        Map<String, Map<String, Set<Finding>>> onlyInWithRule = findOnlyInOneMap(findingWithRule, findingWithoutRule);

        System.out.println("finding only in pattern only: ");
        int count = printFindingMap(onlyInWithoutRule);
        System.out.println("finding only in pattern only: total "+count+".");

        System.out.println("finding only in combined with rule: ");
        count = printFindingMap(onlyInWithRule);
        System.out.println("finding only in combined with rule: total "+count+".");
    }

    private int printFindingMap(Map<String, Map<String, Set<Finding>>> map){
        int count = 0;
        for(String project : map.keySet()) {
            for (String version : map.get(project).keySet()) {
                for(Finding finding : map.get(project).get(version)){
                    System.out.printf("%s, %s, %s, %s, %s\n", project, version, finding.bugId, finding.api, finding.violationType);
                    count++;
                }
            }
        }
        return count;
    }

    //only in map1
    private Map<String, Map<String, Set<Finding>>> findOnlyInOneMap(Map<String, Map<String, Set<Finding>>> map1, Map<String, Map<String, Set<Finding>>> map2){
        Map<String, Map<String, Set<Finding>>> onlyInMap1 = new HashMap<>();
        for(String project : map1.keySet()){
            for(String version : map1.get(project).keySet()){
                for(Finding finding1 : map1.get(project).get(version)){
                    boolean exist = false;
                    if(map2.get(project).get(version)!=null) {
                        for (Finding finding2 : map2.get(project).get(version)) {
                            if (finding1.file.equals(finding2.file) && finding2.method.equals(finding2.method)) {
                                exist = true;
                                break;
                            }
                        }
                    }
                    if(!exist){
                        onlyInMap1.putIfAbsent(project, new HashMap<>());
                        onlyInMap1.get(project).putIfAbsent(version, new LinkedHashSet<>());
                        onlyInMap1.get(project).get(version).add(finding1);
                    }
                }
            }
        }
        return onlyInMap1;
    }

    private Map<String, Map<String, Set<Finding>>> loadFinding(String resultDir) throws Exception {
        Map<String, Map<String, Set<Finding>>> findingMap = new HashMap<>();
        for(File project : new File(resultDir).listFiles()){
            if(!project.isDirectory()) continue;
            Map<String, Set<Finding>> versionMap = new HashMap<>();
            for(File version : project.listFiles()){
                if(!project.isDirectory()) continue;
                File findingOutYml = Paths.get(version.getAbsolutePath(), "findings-output.yml").toFile();
                if(!findingOutYml.exists()){
                    continue;
                }
                Yaml yaml = new Yaml();
                Iterable<Object> iterator = yaml.loadAll(new FileInputStream(findingOutYml));
                for(Object object : iterator){
                    Map<String, Object> o = (Map<String, Object>)object;
                    Finding finding = new Finding();
                    finding.project = project.getName();
                    finding.version = version.getName();
                    finding.file = (String)o.get("file");
                    finding.method = (String)o.get("method");
                    finding.pattern_violation = (String)o.get("pattern_violation");
                    finding.target_environment_mapping = (String)o.get("target_environment_mapping");
                    finding.pattern_examples = ((List<String>)o.get("pattern_examples")).toArray(new String[0]);
                    if(targetProjectMap.get(finding.project)==null || targetProjectMap.get(finding.project).get(finding.version)==null){
                        System.out.println(finding.project+" "+finding.version);
                    }
                    for (Misuse misuse : targetProjectMap.get(finding.project).get(finding.version).getMisuses()) {
                        if (finding.file.contains(misuse.getFilePath())) {
                            finding.bugId = misuse.getId();
                            finding.api = String.join(" ", misuse.getMisusedAPI().stream().map(API::getName).collect(Collectors.toList()).toArray(new String[0]));
                            finding.violationType = String.join(" ", misuse.getViolationTypes().stream().map(Enum::name).collect(Collectors.toList()));
                            versionMap.putIfAbsent(version.getName(), new LinkedHashSet<>());
                            versionMap.get(version.getName()).add(finding);
                            break;
                        }
                    }
                }
            }
            findingMap.put(project.getName(), versionMap);
        }
        return findingMap;
    }


    static class Finding{
        String project;
        String version;
        String bugId;
        String file;
        String method;
        String api;
        String pattern_violation;
        String target_environment_mapping;
        String[] pattern_examples;
        String violationType;

        @Override
        public int hashCode(){
            if(bugId==null){
                return super.hashCode();
            }
            return project.hashCode() + version.hashCode() + bugId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj){
                return true;
            }
            if(! (obj instanceof Finding)){
                return false;
            }
            Finding f = (Finding) obj;
            if(this.project.equals(f.project)
                    && this.version.equals(f.version)
                    && this.bugId.equals(f.bugId)){
                return true;
            }
            return false;
        }

    }
}
