package de.tu_darmstadt.stg.mudetect;

import de.tu_darmstadt.stg.mudetect.aug.model.ActionNode;
import de.tu_darmstadt.stg.mudetect.aug.model.Edge;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.MethodCallNode;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.NullCheckNode;
import de.tu_darmstadt.stg.mudetect.aug.model.controlflow.OrderEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.data.LiteralNode;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.model.Overlap;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MissingNullCheckLiteralNoViolationPredicate implements ViolationPredicate {
    @Override
    public Optional<Boolean> apply(Overlap overlap) {
        return isMissingNullCheckLiteral(overlap) ? Optional.of(false) : Optional.empty();
    }

    private boolean isMissingNullCheckLiteral(Overlap overlap) {
        APIUsagePattern pattern = overlap.getPattern();
        if(!isNullCheckPattern(pattern)){
            return false;
        }

        boolean missNullCheck = false;
        //获取nullcheck对应的调用节点
        Node targetCallNode = null;
        Set<Node> missingPatternNodes = overlap.getMissingNodes();
        for(Node node : missingPatternNodes){
            if(node instanceof NullCheckNode){
                missNullCheck = true;
                if(!overlap.getPattern().incomingNodesOf(node).isEmpty()){
                    Node paramNode = overlap.getPattern().incomingNodesOf(node).iterator().next();
                    Node patternCallNode = null;
                    for (Node n : overlap.getPattern().outgoingNodesOf(paramNode)){
                        if(n instanceof MethodCallNode){
                            patternCallNode = n;
                            break;
                        }
                    }
                    if(patternCallNode == null){
                        return false;
                    }
                    targetCallNode = overlap.getMappedTargetNode(patternCallNode);
                }
            }else if(node instanceof ActionNode){
                return false;
            }
        }
        if(!missNullCheck || targetCallNode == null){
            return false;
        }
        Set<Node> redundantNodes = overlap.getRedundantNodes();
        Set<Edge> incomingNodes = new HashSet<>();
        try {
            incomingNodes = overlap.getTarget().incomingEdgesOf(targetCallNode);
        }catch (Exception e){
        }
        for(Node node : redundantNodes) {
            if (node instanceof LiteralNode && incomingNodes.contains(node)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNullCheckPattern(APIUsagePattern pattern){
        for(Node node : pattern.vertexSet()){
            if(node instanceof NullCheckNode){
                return true;
            }
        }
        return false;
    }

}
