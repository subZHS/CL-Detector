package de.tu_darmstadt.stg.mubench;


import de.tu_darmstadt.stg.sourcerule.SourceRuleParser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class TargetProject {
    private final String projectId;
    private final String versionId;
    private final String[] codePath;
    private Collection<Misuse> misuses = new ArrayList<>();

    public static String targetProjectBasePath = "/Users/hsz/zhs/sjtu/APIMisuse/dataset/MUBench/targetProjects";

    //TODO (myCode)
    static String getTargetProjectPath(String[] line){
        return targetProjectBasePath + getTargetProjectRelativePath(line);
    }

    static String getTargetProjectRelativePath(String[] line){
//        if(line[1].equals("jigsaw")){
//            line[2] = line[2].split("\\|")[1];
//        }
        return String.format("/%s/%s", line[1], line[2]);
    }

    static Set<String> getAllMuBenchAPI() throws IOException {
        Path index = CrossProjectStrategy.getIndexFilePath();
        Stream<String> lines = Files.lines(index);
        Set<String> apis = lines.filter(line -> !line.isEmpty())
                .map(line -> line.split("\t")[6])
                .filter(line -> line.contains(".")).collect(Collectors.toSet());
        for(String s : new HashSet<>(apis)){
            if(s.contains(" ")){
                apis.remove(s);
                apis.addAll(Arrays.asList(s.split(" ")));
            }
        }
        return apis;
    }

    static Set<String> getAllMuBenchTargetProjectId() throws IOException {
        Path index = CrossProjectStrategy.getIndexFilePath();
        Stream<String> lines = Files.lines(index);
        return lines.filter(line -> !line.isEmpty())
                .map(line -> line.split("\t")[1]).collect(Collectors.toSet());
    }

    static boolean contain(List<TargetProject> targetProjectList, String[] line){
        List<TargetProject> resultProjects = targetProjectList.stream().filter(targetProject ->
                targetProject.getProjectId().equals(line[0]) && targetProject.getVersionId().equals(parseVersionId(line))
        ).collect(Collectors.toList());
        return !CollectionUtils.isEmpty(resultProjects);
    }

    static String parseVersionId(String[] split){
        if(split[1].equals("jigsaw")){
            return split[2].split("\\|")[1];
        }
        return split[2];
    }

    static List<TargetProject> find(Path index, String[] targetSrcPaths) throws IOException {
        try (Stream<String> lines = Files.lines(index)) {
            Map<TargetProject, List<Misuse>> collect = lines
                    .filter(line -> !line.isEmpty())
                    //TODO (myCode)
                    .map(line -> {
                        String[] split = line.split("\t");
                        split[2] = parseVersionId(split);
//                        if(split[1].equals("jigsaw")){
//                            split[2] = split[2].split("\\|")[1];
//                        }
                        return split;
                    })
                    .filter(line -> {
                        if(targetSrcPaths == null){
                            return true;
                        }
                        return anyContains(targetSrcPaths, getTargetProjectRelativePath(line));
                    })
                    .collect(
                            Collectors.groupingBy(
                                    line -> new TargetProject(line[1], line[2],
                                            targetSrcPaths != null? targetSrcPaths : new String[]{getTargetProjectPath(line)}),
                                    Collectors.mapping(
                                            // TODO use all target types (line[7+])
                                            line -> new Misuse(line[3], line[4], line[5], line[6], line[7]),
                                            Collectors.toList()
                                    )
                            )
                    );

            if (collect.isEmpty()) {
                throw new IllegalStateException(
                        String.format("Found no target project for paths [%s]", toString(targetSrcPaths))
                );
            }

            //TODO (myCode)
//            if (collect.size() > 1) {
//                throw new IllegalStateException(
//                        String.format("Found more than one target project for paths [%s]: %s",
//                                toString(targetSrcPaths),
//                                collect.keySet().stream().map(TargetProject::getId).collect(joining(", "))
//                        ));
//            }

//            Map.Entry<TargetProject, List<Misuse>> data = collect.entrySet().iterator().next();
            List<TargetProject> targetProjectList = new ArrayList<>();
            for(Map.Entry<TargetProject, List<Misuse>> data : collect.entrySet()){
                TargetProject project = data.getKey();
                project.setMisuses(data.getValue());
                targetProjectList.add(project);
            }
            return targetProjectList;
        }
    }

    public static boolean anyContains(String[] strings, String substring) {
        for (String string : strings) {
            if (string.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    private static String toString(String[] strings) {
        return Arrays.stream(strings).collect(joining(", "));
    }

    private TargetProject(String projectId, String versionId, String[] codePath) {
        this.projectId = projectId;
        this.versionId = versionId;
        this.codePath = codePath;
    }

    public TargetProject(String projectId, String versionId) {
        this.projectId = projectId;
        this.versionId = versionId;
        this.codePath = new String[]{Paths.get(targetProjectBasePath, projectId, versionId).toString()};
    }

    public TargetProject(TargetProject project){
        this.projectId = project.projectId;
        this.versionId = project.versionId;
        this.codePath = project.codePath;
    }

    public String getId() {
        return String.format("%s.%s", getProjectId(), getVersionId());
    }

    public String getProjectId() {
        return projectId;
    }

    public String getVersionId() {
        return versionId;
    }

    public String[] getCodePath() {
        return codePath;
    }

    public Collection<Misuse> getMisuses() {
        return misuses;
    }

    private void setMisuses(Collection<Misuse> misuses) {
        this.misuses = misuses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetProject that = (TargetProject) o;
        if(!that.projectId.equals(this.projectId)) return false;
        if(!that.versionId.equals(this.versionId)) return false;
        return Arrays.equals(codePath, that.codePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, versionId);
    }
}
