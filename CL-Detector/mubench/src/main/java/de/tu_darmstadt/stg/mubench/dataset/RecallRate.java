package de.tu_darmstadt.stg.mubench.dataset;

import de.tu_darmstadt.stg.mubench.API;
import de.tu_darmstadt.stg.mubench.Misuse;
import de.tu_darmstadt.stg.mubench.TargetProject;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class RecallRate {

    private List<TargetProject> foundMisuseProjectList;
    private List<TargetProject> totalMisuseProjectList;
    private Integer foundNum;
    private Integer totalNum;
    private Integer projectNum;
    private Integer apiNum;

    public RecallRate(List<TargetProject> foundMisuseProjectList, List<TargetProject> totalMisuseProjectList){
        this.foundMisuseProjectList = foundMisuseProjectList;
        this.totalMisuseProjectList = totalMisuseProjectList;
    }

    public RecallRate(Integer foundNum, Integer totalNum){
        this.foundNum =foundNum;
        this.totalNum = totalNum;
    }

    public double getRecallRate(){
        return getFoundNum() * 1.0 / getTotalNum();
    }

    public String getFormula(){
        return String.format("%d / %d", getFoundNum(), getTotalNum());
    }

    public Collection<Misuse> getFoundMisuses(){
        return foundMisuseProjectList.stream().map(TargetProject::getMisuses)
                .reduce(new ArrayList<>(), (a, b) -> {a.addAll(b);return a;});
    }

    public Collection<Misuse> getTotalMisuses(){
        return totalMisuseProjectList.stream().map(TargetProject::getMisuses)
                .reduce(new ArrayList<>(), (a, b) -> {a.addAll(b);return a;});
    }

    public List<Map.Entry<String, RecallRate>> getAPIRecallMap(){
        Collection<Misuse> totalMisuseList = getTotalMisuses();
        Collection<Misuse> foundMisuseList = getFoundMisuses();

        Map<String, Integer> apiTotalNumMap = calculateAPIMisuseNum(totalMisuseList);

        Map<String, Integer> apiFoundNumMap = calculateAPIMisuseNum(foundMisuseList);

        Map<String, RecallRate> map = new LinkedHashMap<>();
        for(String api : apiTotalNumMap.keySet()){
            RecallRate recallRate = new RecallRate(apiFoundNumMap.getOrDefault(api, 0), apiTotalNumMap.get(api));
            map.put(api, recallRate);
        }

        //按召回率排序
        List<Map.Entry<String, RecallRate>> apiRecallEntryList = new ArrayList<>(map.entrySet());
        apiRecallEntryList.sort(Comparator.comparing(e -> ((Map.Entry<String, RecallRate>)e).getValue().getRecallRate()).reversed());

        return apiRecallEntryList;
    }

    private Map<String, Integer> calculateAPIMisuseNum(Collection<Misuse> misuses){
        Map<String, Integer> result = new HashMap<>();
        for(Misuse misuse : misuses){
            for(API api : misuse.getMisusedAPI()){
                String name = api.getName();
                result.putIfAbsent(name, 0);
                result.put(name, result.get(name)+1);
            }
        }
        return result;
    }

    public List<Map.Entry<String, RecallRate>> getProjectRecallMap(){
        Map<String, RecallRate> map = new LinkedHashMap<>();
        Map<String, List<TargetProject>> nameProjectListMap = totalMisuseProjectList.stream().collect(Collectors.groupingBy(
                TargetProject::getProjectId, Collectors.toList()));
        for(String projectName : nameProjectListMap.keySet()){
            int totalNum = nameProjectListMap.get(projectName).stream().mapToInt(project->project.getMisuses().size()).sum();
            int foundNum = foundMisuseProjectList.stream().filter(project -> project.getProjectId().equals(projectName)).
                    mapToInt(project->project.getMisuses().size()).sum();
            RecallRate recallRate = new RecallRate(foundNum, totalNum);
            map.put(projectName, recallRate);
        }
        //按召回率排序
        List<Map.Entry<String, RecallRate>> projectRecallEntryList = new ArrayList<>(map.entrySet());
        projectRecallEntryList.sort(Comparator.comparing(e -> ((Map.Entry<String, RecallRate>)e).getValue().getRecallRate()).reversed());

        return projectRecallEntryList;
    }

    public Integer getTotalNum() {
        if(totalNum == null){
            totalNum = totalMisuseProjectList.stream().map(project -> project.getMisuses().size()).reduce(0, Integer::sum);
        }
        return totalNum;
    }

    public Integer getFoundNum() {
        if(foundNum==null){
            foundNum = foundMisuseProjectList.stream().map(project -> project.getMisuses().size()).reduce(0, Integer::sum);
        }
        return foundNum;
    }

    public Integer getNotFoundNum(){
        return getTotalNum() - getFoundNum();
    }

    public Integer getProjectNum() {
        if(projectNum == null){
            projectNum = totalMisuseProjectList.size();
        }
        return projectNum;
    }

    public Integer getAPINum(){
        if(apiNum == null){
            Collection<Misuse> totalMisuseList = totalMisuseProjectList.stream().map(TargetProject::getMisuses)
                    .reduce(new ArrayList<>(), (a, b) -> {a.addAll(b);return a;});
            apiNum = calculateAPIMisuseNum(totalMisuseList).keySet().size();
        }
        return apiNum;
    }

    public void printAllMisusesTaggedIfFound(String csvPath) throws Exception {
        List<List<String>> csvLines = new ArrayList<>();
        Map<String, Map<String, Collection<Misuse>>> foundMisuseMap = foundMisuseProjectList.stream().collect(Collectors.groupingBy(
                TargetProject::getProjectId, Collectors.toMap(TargetProject::getVersionId, TargetProject::getMisuses)));
        for(TargetProject project : totalMisuseProjectList){
            Collection<Misuse> foundMisuses = new ArrayList<>();
            if(foundMisuseMap.get(project.getProjectId()) != null
                    && foundMisuseMap.get(project.getProjectId()).get(project.getVersionId()) != null){
                foundMisuses = foundMisuseMap.get(project.getProjectId()).get(project.getVersionId());
            }
            for(Misuse misuse : project.getMisuses()){
                boolean found = false;
                for(Misuse foundMisuse : foundMisuses){
                    if(foundMisuse.getId().equals(misuse.getId())){
                        found = true;
                        break;
                    }
                }
                List<String> lines = Arrays.asList(project.getProjectId(), project.getVersionId(), misuse.getId(),
                        misuse.getFilePath(), misuse.getMethodSignature(), String.join(" ", misuse.getMisusedAPI().stream().map(API::getName).collect(Collectors.toList())),
                        String.join(" ", misuse.getViolationTypes().stream().map(Enum::name).collect(Collectors.toList())),
                        String.valueOf(found));
                csvLines.add(lines);
            }
        }
        writeLineToCsv(csvLines, csvPath);
    }

    public static void writeLineToCsv(List<List<String>> lines, String csvPath) throws Exception {
        FileWriter writer= new FileWriter(csvPath, false);
        for(int i=0;i<lines.size();i++){
            writer.write(String.join("\t", lines.get(i)));
            if(i < lines.size()-1){
                writer.write("\n");
            }
        }
        writer.close();
    }
}
