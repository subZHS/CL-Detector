package de.tu_darmstadt.stg.mudetect.aug.model.patterns;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageGraph;
import de.tu_darmstadt.stg.mudetect.aug.model.Location;

import java.util.*;

public class APIUsagePattern extends APIUsageGraph implements Cloneable {
    private int support;
    private Set<Location> exampleLocations;
    private String ruleId;
    private String ruleApi;

    public APIUsagePattern(int support, Set<Location> exampleLocations) {
        this.support = support;
        this.exampleLocations = exampleLocations;
    }

    public APIUsagePattern(int support, Set<Location> exampleLocations, String ruleId) {
        this.support = support;
        this.exampleLocations = exampleLocations;
        this.ruleId = ruleId;
    }

    public APIUsagePattern(){}

    public int getSupport() {
        return support;
    }

    public Set<Location> getExampleLocations() {
        return exampleLocations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        APIUsagePattern pattern = (APIUsagePattern) o;
        return support == pattern.support;
    }

    //TODO (myCode)
    public boolean containAPINodes(Set<String> apis){
        for(String api : getAPIs()){
            if(apis.contains(api)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), support);
    }

    public void setSupport(int support) {
        this.support = support;
    }

    public void setExampleLocations(Set<Location> exampleLocations) {
        this.exampleLocations = exampleLocations;
    }

    public String getRuleId(){
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleApi() {
        return ruleApi;
    }

    public void setRuleApi(String ruleApi) {
        this.ruleApi = ruleApi;
    }
}
