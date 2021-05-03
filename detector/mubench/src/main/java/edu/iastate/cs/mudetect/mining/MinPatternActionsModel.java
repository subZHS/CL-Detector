package edu.iastate.cs.mudetect.mining;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageGraph;
import de.tu_darmstadt.stg.mudetect.aug.model.Edge;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.ConstructorCallNode;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.MethodCallNode;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.aug.visitors.AUGLabelProvider;
import de.tu_darmstadt.stg.mudetect.aug.visitors.BaseAUGLabelProvider;
import de.tu_darmstadt.stg.sourcerule.SourceRuleParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tu_darmstadt.stg.mudetect.aug.model.Edge.Type.SYNCHRONIZE;
import static de.tu_darmstadt.stg.mudetect.aug.model.Edge.Type.THROW;

public class MinPatternActionsModel implements Model {

    private final Set<APIUsagePattern> patterns;

    public MinPatternActionsModel(Model model, int minNumberOfCalls) {
        List<APIUsagePattern> patternList = model.getPatterns().stream()
                .filter(pattern -> nodeNumberAfterIgnore(pattern) > 1)//TODO (myCode) 在删除无用节点后总节点数大于1
                .filter((pattern) -> hasEnoughCalls(pattern, minNumberOfCalls))
                .collect(Collectors.toList());

        //TODO (myCode)合并同构pattern
        List<APIUsagePattern> finalPatternList = new ArrayList<>();
        for(int i=0; i < patternList.size(); i++){
            boolean hasSame = false;
            for(int j=i+1; j < patternList.size(); j++){
                if(!isDifferentAUG(patternList.get(i), patternList.get(j))){
                    hasSame = true;
                    break;
                }
            }
            if(!hasSame)
                finalPatternList.add(patternList.get(i));
        }
        patterns = new LinkedHashSet<>(finalPatternList);
    }



    //TODO (myCode)
    private int nodeNumberAfterIgnore(APIUsagePattern pattern){
        List<Node> nodeList = new ArrayList<>(pattern.vertexSet());
        nodeList.removeAll(SourceRuleParser.nodeListToIgnore(pattern));
        return nodeList.size();
    }

    //TODO (myCode)
    public static boolean isDifferentAUG(APIUsageGraph example1, APIUsageGraph example2){
        AUGLabelProvider labelProvider = new BaseAUGLabelProvider();
        Set<Node> nodes1 = example1.vertexSet();
        Set<Node> nodes2 = example2.vertexSet();
        Multiset<String> nodesOfLabel1 = HashMultiset.create();
        nodesOfLabel1.addAll(nodes1.stream().map(node -> labelProvider.getLabel(node)).collect(Collectors.toSet()));
        Multiset<String> nodesOfLabel2 = HashMultiset.create();
        nodesOfLabel2.addAll(nodes2.stream().map(node -> labelProvider.getLabel(node)).collect(Collectors.toSet()));
        Multiset<String> node1only = HashMultiset.create(nodesOfLabel1);
        node1only.removeAll(nodesOfLabel2);
        if(!nodesOfLabel1.equals(nodesOfLabel2)){
            return true;
        }

        Set<Edge> edges1 = example1.edgeSet();
        Set<Edge> edges2 = example2.edgeSet();
        if(edges1.size()!=edges2.size()){
            return true;
        }
        Multiset<String> edgesOfLabel1 = HashMultiset.create();
        edgesOfLabel1.addAll(edges1.stream().map(edge -> edgeToString(labelProvider, edge)).collect(Collectors.toSet()));
        Multiset<String> edgesOfLabel2 = HashMultiset.create();
        edgesOfLabel2.addAll(edges2.stream().map(edge -> edgeToString(labelProvider, edge)).collect(Collectors.toSet()));
        if(!edgesOfLabel1.equals(edgesOfLabel2)){
            return true;
        }
        return false;
    }

    //TODO (myCode)
    private static String edgeToString(AUGLabelProvider labelProvider, Edge edge){
        return labelProvider.getLabel(edge.getSource()) +" "+ labelProvider.getLabel(edge) +" "+ labelProvider.getLabel(edge.getTarget());
    }

    private boolean hasEnoughCalls(APIUsagePattern pattern, int minNumberOfCalls) {
        long numberOfCalls = pattern.vertexSet().stream().filter(MinPatternActionsModel::isMethodCall).count();
        //TODO (myCode) 除去构造方法，调用个数大于minNumberOfCalls
        numberOfCalls = numberOfCalls - pattern.vertexSet().stream().filter(node -> node instanceof ConstructorCallNode).count();
        long numberOfThrows = pattern.edgeSet().stream().filter(this::isRelevant).count();
        return numberOfCalls + numberOfThrows >= minNumberOfCalls;
    }

    private boolean isRelevant(Edge edge) {
        return edge.getType() == THROW || edge.getType() == SYNCHRONIZE;
    }

    private static boolean isMethodCall(Node node) {
        return node instanceof MethodCallNode;
    }

    @Override
    public Set<APIUsagePattern> getPatterns() {
        return patterns;
    }
}
