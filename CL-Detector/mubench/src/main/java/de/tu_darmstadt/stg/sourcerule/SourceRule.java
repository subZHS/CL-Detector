package de.tu_darmstadt.stg.sourcerule;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SourceRule {

    public enum SourceRuleType{
        Exception("exception"),
        Condition("condition"),
        Order("order");

        private final String label;

        SourceRuleType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static SourceRuleType fromLabel(String label){
            for(SourceRuleType type: SourceRuleType.values()){
                if(type.getLabel().equals(label)){
                    return type;
                }
            }
            return null;
        }
    }

    private String id;
    private List<APIUsageExample> augList = new ArrayList<>();
    private SourceRuleType type;
    private String apiClass;
    private String apiMethod;

    private String libName;
    private String libVersion;

    private String fileLocation;
    private int startLine;
    private int endLine;

    private String condition;
    private String exception;
    private SourceRule exceptionCondition;
    private List<String> sequence;
    private List<SourceRule> alterOrders;

    public SourceRule(){}

    //deep copy
    public SourceRule(SourceRule rule){
        this.id = rule.id;
        this.augList = rule.getAugList();
        this.type = rule.getType();
        this.apiClass = rule.getApiClass();
        this.apiMethod = rule.getApiMethod();

        this.libName = rule.getLibName();
        this.libVersion = rule.getLibVersion();

        this.fileLocation = rule.getFileLocation();
        this.startLine = rule.getStartLine();
        this.endLine = rule.getEndLine();

        this.condition = rule.getCondition();
        this.exception = rule.getException();
        this.exceptionCondition = rule.getExceptionCondition();
        this.sequence = rule.getSequence();
    }

    public String getLibrarySrcPath(String libRootDir){
        if(libName == null || libVersion == null){
            return null;
        }
        String libDirName = libName + "_" + libVersion;
        return Paths.get(libRootDir, libDirName, "source").toString();
    }

    public String getLibraryJarPath(String libRootDir){
        if(libName == null || libVersion == null){
            return null;
        }
        String libDirName = libName + "_" + libVersion;
        return Paths.get(libRootDir, libDirName, "jar").toString();
    }

    public static SourceRule get(List<SourceRule> sourceRuleList, String ruleId){
        if(sourceRuleList==null) return null;
        for(SourceRule rule : sourceRuleList){
            if(rule.getId().equals(ruleId)){
                return rule;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<APIUsageExample> getAugList() {
        return augList;
    }

    public void setAugList(List<APIUsageExample> augList) {
        this.augList = augList;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public SourceRuleType getType() {
        return type;
    }

    public void setType(SourceRuleType type) {
        this.type = type;
    }

    public String getApiClass() {
        return apiClass;
    }

    public void setApiClass(String apiClass) {
        this.apiClass = apiClass;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public void setApiMethod(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public String getLibName() {
        return libName;
    }

    public void setLibName(String libName) {
        this.libName = libName;
    }

    public String getLibVersion() {
        return libVersion;
    }

    public void setLibVersion(String libVersion) {
        this.libVersion = libVersion;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public SourceRule getExceptionCondition() {
        return exceptionCondition;
    }

    public void setExceptionCondition(SourceRule exceptionCondition) {
        this.exceptionCondition = exceptionCondition;
    }

    public List<String> getSequence() {
        return sequence;
    }

    public void setSequence(List<String> sequence) {
        this.sequence = sequence;
    }

    public List<SourceRule> getAlterOrders() {
        return alterOrders;
    }

    public void setAlterOrders(List<SourceRule> alterOrders) {
        this.alterOrders = alterOrders;
    }
}
