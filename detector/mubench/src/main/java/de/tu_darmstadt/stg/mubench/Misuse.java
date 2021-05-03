package de.tu_darmstadt.stg.mubench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Misuse {
    private final String id;
    private final String filePath;
    private final String methodSignature;
    private List<API> api;
    private List<ViolationType> violationTypes = new ArrayList<>();

    public enum ViolationType{
        MissCall, MissCondition, MissException, RedundantCall, RedundantCondition, RedundantException;
    }

    public Misuse(String id, String filePath, String methodSignature, String api, String violationTypeStr) {
        //TODO (myCode)
        if(methodSignature.startsWith("\"") && methodSignature.endsWith("\"")){
            methodSignature = methodSignature.replace("\"", "").replace("\"", "");
        }
        List<String> apis = Arrays.asList(api.split(" "));
        this.id = id;
        this.filePath = filePath;
        this.methodSignature = methodSignature;
        this.api = apis.stream().map(API::new).collect(Collectors.toList());

        for(String violationType : violationTypeStr.split(" ")){
            if(violationType.equals("missing_exception_handling")){
                violationTypes.add(ViolationType.MissException);
            }else if(violationType.equals("missing_condition")){
                violationTypes.add(ViolationType.MissCondition);
            }else if(violationType.equals("missing_call")){
                violationTypes.add(ViolationType.MissCall);
            }else if(violationType.equals("redundant_call")){
                violationTypes.add(ViolationType.RedundantCall);
            }else if(violationType.equals("redundant_condition")){
                violationTypes.add(ViolationType.RedundantCondition);
            }else if(violationType.equals("redundant_exception_handling")){
                violationTypes.add(ViolationType.RedundantException);
            }
        }
    }

    public String getId() {
        return id;
    }

    public boolean isIn(String sourceFilePath) {
        return sourceFilePath.endsWith(filePath);
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public List<API> getMisusedAPI() {
        return api;
    }

    public void setApi(List<API> api) {
        this.api = api;
    }

    public List<ViolationType> getViolationTypes() {
        return violationTypes;
    }

    public void setViolationTypes(List<ViolationType> violationTypes) {
        this.violationTypes = violationTypes;
    }
}
