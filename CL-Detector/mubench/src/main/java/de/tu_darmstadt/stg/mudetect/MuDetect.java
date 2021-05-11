package de.tu_darmstadt.stg.mudetect;

import de.tu_darmstadt.stg.mubench.expand.PatternExpander;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.MethodCallNode;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.model.*;
import de.tu_darmstadt.stg.sourcerule.SourceRule;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.mudetect.mining.Model;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.tu_darmstadt.stg.sourcerule.SourceRule.SourceRuleType;
import org.apache.commons.collections4.CollectionUtils;

public class MuDetect {

    private final Model model;
    private final OverlapsFinder overlapsFinder;
    private final ViolationPredicate violationPredicate;
    private final BiFunction<Overlaps, Model, List<Violation>> filterAndRankingStrategy;

    public MuDetect(Model model,
                    OverlapsFinder overlapsFinder,
                    ViolationPredicate violationPredicate,
                    BiFunction<Overlaps, Model, List<Violation>> filterAndRankingStrategy) {
        this.model = model;
        this.overlapsFinder = overlapsFinder;
        this.violationPredicate = violationPredicate;
        this.filterAndRankingStrategy = filterAndRankingStrategy;
    }

    public List<Violation> findViolations(Collection<APIUsageExample> targets) {
        return findViolations(targets, null);
    }

    //TODO (myCode)添加sourceRuleList
    public List<Violation> findViolations(Collection<APIUsageExample> targets, List<SourceRule> sourceRuleList) {
        final Overlaps overlaps = findOverlaps(targets, model.getPatterns(), sourceRuleList);
        return filterAndRankingStrategy.apply(overlaps, model);
    }

    //TODO (myCode)添加alternative rules
    private Overlaps findOverlaps(Collection<APIUsageExample> targets, Set<APIUsagePattern> patterns, List<SourceRule> sourceRuleList) {
        Overlaps overlaps = new Overlaps(sourceRuleList);
        int ruleSupport = patterns.stream().map(APIUsagePattern::getSupport).max(Comparator.comparing(i->i)).orElse(1);
        //挑选con/exc rule作为alternative rules
//        Map<String, Map<String, List<SourceRule>>> alterRuleMap = null;
//        if(!CollectionUtils.isEmpty(sourceRuleList)) {
//            alterRuleMap = sourceRuleList.stream().filter(rule ->
//                    Arrays.asList(SourceRuleType.Exception, SourceRuleType.Condition).contains(rule.getType())).collect(
//                    Collectors.groupingBy(rule-> AUGBuilder.toSimpleTypeName(rule.getApiClass()), Collectors.groupingBy(SourceRule::getApiMethod, Collectors.toList())));
//        }
        for (APIUsageExample target : targets) {
            for (APIUsagePattern pattern : patterns) {
                for (Overlap overlap : overlapsFinder.findOverlaps(target, pattern)) {
                    if (violationPredicate.apply(overlap).orElse(false)) {
                        if(isNotAlternativeRuleInstance(overlap, overlaps, ruleSupport)){
                            overlaps.addViolation(overlap);
                        }
                    } else {
                        overlaps.addInstance(overlap);
                    }
                }
            }

            //创建alternative rules的instances
//            if(alterRuleMap == null){
//                continue;
//            }
//            Set<Node> callNodes = target.vertexSet().stream().filter(node -> node instanceof MethodCallNode).collect(Collectors.toSet());
//
//            for(Node node : callNodes){
//                MethodCallNode callNode = (MethodCallNode) node;
//                String methodSig = callNode.getMethodSignature();
//                String apiClass = callNode.getDeclaringTypeName();
//                List<SourceRule> alterRuleList = alterRuleMap.getOrDefault(apiClass, new HashMap<>()).get(methodSig);
//                if(CollectionUtils.isEmpty(alterRuleList)){
//                    continue;
//                }
//                List<APIUsagePattern> rulePatternList = PatternExpander.ruleToPatterns(alterRuleList, ruleSupport);
//                for (APIUsagePattern pattern : rulePatternList) {
//                    for (Overlap overlap : overlapsFinder.findOverlaps(target, pattern)) {
//                        if (!violationPredicate.apply(overlap).orElse(false)) {
//                            overlaps.addInstance(overlap);
//                        }
//                    }
//                }
//            }
        }
        return overlaps;
    }


    private boolean isNotAlternativeRuleInstance(Overlap violation, Overlaps overlaps, int support){
        String ruleId = violation.getPattern().getRuleId();
        if(ruleId==null)  return true;
        SourceRule rule = SourceRule.get(overlaps.getSourceRuleList(), ruleId);
        if(rule==null)  return true;
        Set<SourceRule> alterRules = overlaps.findAlterRuleList(rule);
        if(alterRules == null) return true;
        for(SourceRule alterRule : alterRules){
            if(alterRule.getId().equals(ruleId)) continue;
            List<APIUsagePattern> alterRulePatternList = PatternExpander.ruleToPatterns(Arrays.asList(alterRule), support);
            for(APIUsagePattern pattern : alterRulePatternList){
                for (Overlap overlap : overlapsFinder.findOverlaps(violation.getTarget(), pattern)) {
                    Optional<Boolean> isViolate = violationPredicate.apply(overlap);
                    if (isViolate.isPresent() && isViolate.get().equals(false)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

