package de.tu_darmstadt.stg.mudetect;

import de.tu_darmstadt.stg.mudetect.aug.model.Edge;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.CatchNode;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.MethodCallNode;
import de.tu_darmstadt.stg.mudetect.aug.model.controlflow.OrderEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.controlflow.SelectionEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.model.Overlap;

import java.util.Collection;

public class AlternativePatternInstancePredicate {
    public boolean test(Overlap violation, Collection<Overlap> instances) {
        boolean isOrderPattern = onlyOrderPattern(violation.getPattern());
        for (Overlap instance : instances) {
            if(isOrderPattern ^ onlyOrderPattern(instance.getPattern())){
                //order pattern和非order pattern不互为alternative
                continue;
            }
            if (violation.isInTargetOverlap(instance)) {
                return true;
            }
        }
        return false;
    }

    public static boolean onlyOrderPattern(APIUsagePattern pattern){
        boolean hasCatch = false;
        boolean hasCondtion = false;
        boolean hasOrder = false;
        for(Node node : pattern.vertexSet()){
            if(node instanceof CatchNode){
                hasCatch = true;
                break;
            }
        }
        for(Edge edge : pattern.edgeSet()) {
            if (edge instanceof OrderEdge
                    && pattern.getEdgeSource(edge) instanceof MethodCallNode
                    && pattern.getEdgeTarget(edge) instanceof MethodCallNode) {
                hasOrder = true;
            }else if(edge instanceof SelectionEdge){
                hasCondtion = true;
            }
        }
        return !hasCatch && !hasCondtion && hasOrder;
    }
}
