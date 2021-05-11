package de.tu_darmstadt.stg.mudetect;

import de.tu_darmstadt.stg.mudetect.aug.model.Edge;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.MethodCallNode;
import de.tu_darmstadt.stg.mudetect.aug.model.controlflow.OrderEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.model.Overlap;

import java.util.Optional;

public class IncorrectOrderViolationPredicate implements ViolationPredicate {
    @Override
    public Optional<Boolean> apply(Overlap overlap) {
        return isIncorrectOrder(overlap) ? Optional.of(true) : Optional.empty();
    }

    private boolean isIncorrectOrder(Overlap overlap) {
        APIUsagePattern pattern = overlap.getPattern();
        MethodCallNode callSource = null, callTarget = null;
        for(Edge edge : pattern.edgeSet()) {
            if (edge instanceof OrderEdge
                    && pattern.getEdgeSource(edge) instanceof MethodCallNode
                    && pattern.getEdgeTarget(edge) instanceof MethodCallNode) {
                callSource = (MethodCallNode) pattern.getEdgeSource(edge);
                callTarget = (MethodCallNode) pattern.getEdgeTarget(edge);
                break;
            }
        }
        if(callSource==null){
            return false;
        }
        MethodCallNode source = (MethodCallNode) overlap.getMappedTargetNode(callSource);
        MethodCallNode target = (MethodCallNode) overlap.getMappedTargetNode(callTarget);

        if(source!=null && target!=null){
            boolean correctOrder = false;
            for(Edge edge : overlap.getTarget().outgoingEdgesOf(source)){
                if(edge instanceof OrderEdge && overlap.getTarget().getEdgeTarget(edge).equals(target)){
                    correctOrder = true;
                    break;
                }
            }
            return !correctOrder;
        }
        return false;
//        if(!isOrderPattern(pattern)){
//            return false;
//        }
//        return overlap.getMappedTargetNodes().size() == 2 && overlap.getMappedTargetEdges().size() == 0;
    }

    public static boolean isOrderPattern(APIUsagePattern pattern){
        for(Edge edge : pattern.edgeSet()) {
            if (edge instanceof OrderEdge
                    && pattern.getEdgeSource(edge) instanceof MethodCallNode
                    && pattern.getEdgeTarget(edge) instanceof MethodCallNode) {
                return true;
            }
        }
        return false;
//        if(pattern.edgeSet().size()==1 && pattern.vertexSet().size() == 2){
//            return false;
//        }
//        for(Edge edge : pattern.edgeSet()){
//            if(! (edge instanceof OrderEdge)){
//                return false;
//            }
//        }
//        for(Node node : pattern.vertexSet()){
//            if(!(node instanceof MethodCallNode)){
//                return false;
//            }
//        }
//        return true;
    }

}
