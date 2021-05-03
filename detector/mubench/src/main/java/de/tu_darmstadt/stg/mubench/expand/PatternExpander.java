package de.tu_darmstadt.stg.mubench.expand;

import de.tu_darmstadt.stg.mudetect.aug.builder.APIUsageExampleBuilder;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.Edge;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.CatchNode;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.MethodCallNode;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.OperatorNode;
import de.tu_darmstadt.stg.mudetect.aug.model.data.ExceptionNode;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.ParameterEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.sourcerule.SourceRule;
import de.tu_darmstadt.stg.sourcerule.SourceRuleParser;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.egroum.aug.EGroumNode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import de.tu_darmstadt.stg.sourcerule.SourceRule.SourceRuleType;

public class PatternExpander {

    public static List<APIUsagePattern> expandNewPattern(List<APIUsagePattern> patternList, List<SourceRule> sourceRuleList){

        Map<String, Map<String, List<SourceRule>>> sourceRuleMap = sourceRuleList.stream().collect(
                Collectors.groupingBy(rule-> AUGBuilder.toSimpleTypeName(rule.getApiClass()), Collectors.groupingBy(SourceRule::getApiMethod, Collectors.toList())));
        Set<String> simpleNameAPIs = sourceRuleList.stream().map(rule -> AUGBuilder.toSimpleTypeName(rule.getApiClass())).collect(Collectors.toSet());

        //总体上是，将pattern和condition/exception rule融合得到新pattern
        //将pattern中的condition/exception节点用condition/exception rule替换，得到新pattern，新pattern和旧pattern为alternative
        List<APIUsagePattern> newPatternList = new ArrayList<>();
        for(APIUsagePattern pattern : patternList){
            //复制pattern用于构建新pattern
            pattern = clonePattern(pattern);

            //确定API调用节点，如果没有则跳过
            List<MethodCallNode> callNodes = new ArrayList<>();
            for(Node node : new ArrayList<>(pattern.vertexSet())){
                if(node instanceof MethodCallNode
                        && simpleNameAPIs.contains(((MethodCallNode) node).getDeclaringTypeName())){
                    callNodes.add((MethodCallNode)node);
                }
            }
            if(callNodes.isEmpty()){
                continue;
            }

            for(MethodCallNode callNode : callNodes){
                String methodSig = callNode.getMethodSignature();
                String apiClass = callNode.getDeclaringTypeName();
                //获取对应API方法的con/exc规则
                List<SourceRule> relatedSourceRules = sourceRuleMap.getOrDefault(apiClass, new HashMap<>()).get(methodSig);
                if(CollectionUtils.isEmpty(relatedSourceRules)){
                    continue;
                }else{
                    relatedSourceRules = relatedSourceRules.stream().filter(r ->
                            r.getType().equals(SourceRuleType.Condition) || r.getType().equals(SourceRuleType.Exception)).collect(Collectors.toList());
                    if(CollectionUtils.isEmpty(relatedSourceRules)){
                        continue;
                    }
                }

                //识别并移除目标调用节点的condition和exception节点
                List<Node> conNodes = extractConNodes(pattern, callNode);
                List<Node> exceptionNodes = extractExceptionNodes(pattern, callNode);

                conNodes.addAll(exceptionNodes);
                for(Node node : conNodes){
                    pattern.removeVertex(node);
                }
                pattern = (APIUsagePattern) SourceRuleParser.removeAloneNodes(pattern);

                //移除con/exc节点后的pattern与con/exc rule合并得到新pattern
                for(SourceRule rule : relatedSourceRules){
                    APIUsageExampleBuilder builder = SourceRuleParser.mergeAUGBuilder(pattern, rule.getAugList().get(0));
                    APIUsagePattern newPattern = builder.buildPattern(pattern.getSupport(), pattern.getExampleLocations());
                    newPatternList.add(newPattern);
                }
            }

        }
        return newPatternList;
    }

    //输入不满足任意pattern的实例，返回违反API源码rule的API使用实例，这些违反rule的实例的排名前置
    public static List<APIUsageExample> judgeViolationOfRules(List<APIUsageExample> exampleList, List<SourceRule> sourceRuleList, String api) {
        List<APIUsageExample> violationList = new ArrayList<>();

        // 必须满足condition/exception rule的其中一个。源码规则中：condition和exception类型的rule互为alternative
        // 判断非null是否满足，如果是参数是primitive类型或者literal，默认非null满足
        // condition的满足感觉需要跨方法的AUG

        // 未满足catch exception，要再判断是否throw exception，以及exception继承关系

        //判断是否违反order规则，需要判断是否违背顺序，一旦违背就是误用

        return violationList;
    }

        //提取跟condition有关的节点，用于替换
    private static List<Node> extractConNodes(APIUsagePattern pattern, Node callNode){
        List<Node> conNodeList = new ArrayList<>();
        for(Node node : new ArrayList<>(pattern.vertexSet())){
            if(node instanceof OperatorNode && pattern.outgoingNodesOf(node).contains(callNode)){
                conNodeList.add(node);
            }
        }
        return conNodeList;
    }

    //提取跟exception有关的节点，用于替换
    private static List<Node> extractExceptionNodes(APIUsagePattern pattern, Node callNode){
        List<Node> exceptionNodeList = new ArrayList<>();
        for(Node node : new ArrayList<>(pattern.vertexSet())){
            if(node instanceof CatchNode && pattern.incomingNodesOf(node).contains(callNode)){
                exceptionNodeList.add(node);
                for(Edge edge : pattern.incomingEdgesOf(node)){
                    if(edge instanceof ParameterEdge && edge.getSource() instanceof ExceptionNode){
                        exceptionNodeList.add(edge.getSource());
                    }
                }
            }
        }
        return exceptionNodeList;
    }
    public static List<APIUsagePattern> ruleToPatterns(List<SourceRule> sourceRuleList, int support){
        List<APIUsagePattern> rulePatternList = new ArrayList<>();
        sourceRuleList.forEach(sourceRule -> rulePatternList.addAll(ruleToPatterns(sourceRule, support)));
        return rulePatternList;
    }

    private static List<APIUsagePattern> ruleToPatterns(SourceRule sourceRule, int support){
        List<APIUsagePattern> rulePatternList = new ArrayList<>();
        for(APIUsageExample aug : sourceRule.getAugList()) {
            APIUsagePattern pattern = new APIUsagePattern(support, new HashSet<>(Arrays.asList(aug.getLocation())), sourceRule.getId());
            pattern.setRuleApi(sourceRule.getApiClass());
            for (Node node : aug.vertexSet()) {
                pattern.addVertex(node);
                node.setGraph(pattern);
            }
            for (Edge edge : aug.edgeSet()) {
                pattern.addEdge(edge.getSource(), edge.getTarget(), edge);
            }
            rulePatternList.add(pattern);
        }
        return rulePatternList;
    }

    public static APIUsagePattern clonePattern(APIUsagePattern pattern){
        APIUsageExampleBuilder builder = APIUsageExampleBuilder.buildAUG();
        Map<Integer, String> oldNewNodeIdMap = new LinkedHashMap<>();
        for(Node node : pattern.vertexSet()){
            String nodeId = EGroumNode.newNodeId();
            builder.withCopyNode(nodeId, node);
            oldNewNodeIdMap.put(node.getId(), nodeId);
        }
        for(Edge edge : pattern.edgeSet()){
            String sourceId = oldNewNodeIdMap.get(edge.getSource().getId());
            String targetId = oldNewNodeIdMap.get(edge.getTarget().getId());
            builder.withCopyEdge(edge, sourceId, targetId);
        }
        return builder.buildPattern(pattern.getSupport(), pattern.getExampleLocations());
    }
}
