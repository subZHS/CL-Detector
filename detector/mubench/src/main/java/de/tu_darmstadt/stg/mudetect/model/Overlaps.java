package de.tu_darmstadt.stg.mudetect.model;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.sourcerule.SourceRule;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Overlaps {
    private Map<APIUsageExample, Set<Overlap>> instancesByTarget = new HashMap<>();
    private Map<APIUsagePattern, Set<Overlap>> violationsByPattern = new HashMap<>();
    private Map<APIUsageExample, Set<Overlap>> violationsByTarget = new HashMap<>();
    private Set<Overlap> violations = new HashSet<>();
    //TODO (myCode)
    private List<SourceRule> sourceRuleList;
    private Map<String, Map<String, Set<Set<SourceRule>>>> alterRuleMap;

    public Overlaps(){}

    //TODO (myCode)
    public Overlaps(List<SourceRule> sourceRuleList){
        this.sourceRuleList = sourceRuleList;
        this.alterRuleMap = new HashMap<>();
        //构建con/exc rule之间的，还有orderRule之间的alternative关系
        if(!CollectionUtils.isEmpty(sourceRuleList)) {

            Map<String, Map<String, Set<SourceRule>>> alterConExpRuleMap = sourceRuleList.stream().filter(rule ->
                    Arrays.asList(SourceRule.SourceRuleType.Exception, SourceRule.SourceRuleType.Condition).contains(rule.getType())).collect(
                    Collectors.groupingBy(SourceRule::getApiClass, Collectors.groupingBy(SourceRule::getApiMethod, Collectors.toSet())));
            for(String apiClass : alterConExpRuleMap.keySet()){
                for(String apiMethod : alterConExpRuleMap.get(apiClass).keySet()){
                    Set<SourceRule> conExpRuleList = alterConExpRuleMap.get(apiClass).get(apiMethod);
                    if(!CollectionUtils.isEmpty(conExpRuleList)){
                        alterRuleMap.putIfAbsent(apiClass, new HashMap<>());
                        alterRuleMap.get(apiClass).putIfAbsent(apiMethod, new HashSet<>());
                        alterRuleMap.get(apiClass).get(apiMethod).add(conExpRuleList);
                    }
                }
            }

            Map<String, Map<String, List<SourceRule>>> orderRuleMap = sourceRuleList.stream().filter(rule -> rule.getType().equals(SourceRule.SourceRuleType.Order))
                    .collect(Collectors.groupingBy(SourceRule::getApiClass, Collectors.groupingBy(SourceRule::getApiMethod, Collectors.toList())));
            for(String apiClass : orderRuleMap.keySet()) {
                for (String apiMethod : orderRuleMap.get(apiClass).keySet()) {
                    List<SourceRule> orderRuleList = orderRuleMap.get(apiClass).get(apiMethod);
                    for(SourceRule orderRule : orderRuleList){
                        alterRuleMap.putIfAbsent(apiClass, new HashMap<>());
                        alterRuleMap.get(apiClass).putIfAbsent(apiMethod, new HashSet<>());
                        if(findContainList(alterRuleMap.get(apiClass).get(apiMethod), orderRule)==null){
                            Set<SourceRule> alterOrderRuleList = new HashSet<>(Arrays.asList(orderRule));
                            if(orderRule.getAlterOrders()!=null) {
                                alterOrderRuleList.addAll(orderRule.getAlterOrders());
                            }
                            alterRuleMap.get(apiClass).get(apiMethod).add(alterOrderRuleList);
                        }
                    }
                }
            }

        }
    }

    public Set<SourceRule> findAlterRuleList(SourceRule rule){
        Set<Set<SourceRule>> alterRulesList = alterRuleMap.getOrDefault(rule.getApiClass(), new HashMap<>()).getOrDefault(rule.getApiMethod(), new HashSet<>());
        Set<SourceRule> alterRules = findContainList(alterRulesList, rule);
        return CollectionUtils.isEmpty(alterRules)? null: alterRules;
    }

    private Set<SourceRule> findContainList(Set<Set<SourceRule>> ruleListList, SourceRule rule){
        for(Set<SourceRule> ruleList : ruleListList){
            Set<String> idList = ruleList.stream().map(SourceRule::getId).collect(Collectors.toSet());
            if(idList.contains(rule.getId())){
                return ruleList;
            }
        }
        return null;
    }

    public Set<Overlap> getInstancesInSameTarget(Overlap overlap) {
        return instancesByTarget.getOrDefault(overlap.getTarget(), Collections.emptySet());
    }

    public Set<Overlap> getViolationsOfSamePattern(Overlap violation) {
        return violationsByPattern.get(violation.getPattern());
    }

    public Set<Overlap> getViolationsInSameTarget(Overlap overlap) {
        return violationsByTarget.get(overlap.getTarget());
    }

    public Set<Overlap> getViolations() {
        return violations;
    }

    public void addViolation(Overlap violation) {
        add(violationsByPattern, violation.getPattern(), violation);
        add(violationsByTarget, violation.getTarget(), violation);
        violations.add(violation);
    }

    public void addInstance(Overlap instance) {
        add(instancesByTarget, instance.getTarget(), instance);
    }

    private <T> void add(Map<T, Set<Overlap>> map, T key, Overlap instance) {
        if (!map.containsKey(key)) {
            map.put(key, new HashSet<>());
        }
        map.get(key).add(instance);
    }

    public void removeViolationIf(Predicate<Overlap> condition) {
        violations.removeIf(condition);
    }

    public int getNumberOfEqualViolations(Overlap violation) {
        int numberOfEqualViolations = 0;
        for (Overlap otherViolation : getViolationsOfSamePattern(violation)) {
            // two overlaps are equal, if they violate the same pattern in the same way,
            // i.e., if the pattern overlap is the same.
            if (violation.isSamePatternOverlap(otherViolation)) {
                numberOfEqualViolations++;
            }
        }
        return numberOfEqualViolations;
    }

    public void mapViolations(Function<Overlap, Overlap> mapping) {
        Set<Overlap> violations = new HashSet<>(this.violations);
        this.violations.clear();
        this.violationsByPattern.clear();
        this.violationsByTarget.clear();
        violations.stream().map(mapping).forEach(this::addViolation);
    }

    public List<SourceRule> getSourceRuleList() {
        return sourceRuleList;
    }

    public Map<String, Map<String, Set<Set<SourceRule>>>> getAlterRuleMap() {
        return alterRuleMap;
    }
}
