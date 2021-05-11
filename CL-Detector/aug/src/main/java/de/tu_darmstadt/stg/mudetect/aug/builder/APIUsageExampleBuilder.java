package de.tu_darmstadt.stg.mudetect.aug.builder;

import de.tu_darmstadt.stg.mudetect.aug.model.*;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.*;
import de.tu_darmstadt.stg.mudetect.aug.model.controlflow.*;
import de.tu_darmstadt.stg.mudetect.aug.model.data.*;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.DefinitionEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.ParameterEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.QualifierEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.ReceiverEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class APIUsageExampleBuilder {
    private final Map<String, Node> nodeMap = new HashMap<>();
    private final Set<Edge> edges = new HashSet<>();
    private final Location location;

    //TODO (myCode)
    public static APIUsageExampleBuilder buildAUGExample(Location location) {
        return new APIUsageExampleBuilder(location);
    }

    //TODO (myCode)
    public static APIUsageExampleBuilder buildAUG() {
        return new APIUsageExampleBuilder(null);
    }

    private APIUsageExampleBuilder(Location location) {
        this.location = location;
    }

    // Action Nodes

    public APIUsageExampleBuilder withArrayAccess(String nodeId, String arrayTypeName, int sourceLineNumber) {
        return withNode(nodeId, new ArrayAccessNode(arrayTypeName, sourceLineNumber));
    }

    public APIUsageExampleBuilder withArrayAssignment(String nodeId, String baseType, int sourceLineNumber) {
        return withNode(nodeId, new ArrayAssignmentNode(baseType, sourceLineNumber));
    }

    public APIUsageExampleBuilder withArrayCreation(String nodeId, String baseType, int sourceLineNumber) {
        return withNode(nodeId, new ArrayCreationNode(baseType, sourceLineNumber));
    }

    public APIUsageExampleBuilder withAssignment(String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new AssignmentNode(sourceLineNumber));
    }

    public APIUsageExampleBuilder withBreak(String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new BreakNode(sourceLineNumber));
    }

    public APIUsageExampleBuilder withCast(String nodeId, String targetType, int sourceLineNumber) {
        return withNode(nodeId, new CastNode(targetType, sourceLineNumber));
    }

    public APIUsageExampleBuilder withConstructorCall(String nodeId, String typeName, int sourceLineNumber) {
        return withNode(nodeId, new ConstructorCallNode(typeName, sourceLineNumber));
    }

    public APIUsageExampleBuilder withConstructorCall(String nodeId, String typeName, String methodSignature, int sourceLineNumber) {
        //TODO (myCode)Constructor要有参数
        return withNode(nodeId, new ConstructorCallNode(typeName, methodSignature, sourceLineNumber));
    }

    public APIUsageExampleBuilder withContinue(String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new ContinueNode(sourceLineNumber));
    }

    public APIUsageExampleBuilder withInfixOperator(String nodeId, String operator, int sourceLineNumber) {
        return withNode(nodeId, new InfixOperatorNode(operator, sourceLineNumber));
    }

    public APIUsageExampleBuilder withMethodCall(String nodeId, String declaringTypeName, String methodSignature, int sourceLineNumber) {
        return withNode(nodeId, new MethodCallNode(declaringTypeName, methodSignature, sourceLineNumber));
    }

    public APIUsageExampleBuilder withNullCheck(String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new NullCheckNode(sourceLineNumber));
    }

    public APIUsageExampleBuilder withReturn(String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new ReturnNode(sourceLineNumber));
    }

    public APIUsageExampleBuilder withSuperConstructorCall(String nodeId, String superTypeName, int sourceLineNumber) {
        return withNode(nodeId, new SuperConstructorCallNode(superTypeName, sourceLineNumber));
    }

    public APIUsageExampleBuilder withSuperConstructorCall(String nodeId, String superTypeName, String methodSignature, int sourceLineNumber) {
        return withNode(nodeId, new SuperConstructorCallNode(superTypeName, methodSignature, sourceLineNumber));
    }

    public APIUsageExampleBuilder withSuperMethodCall(String nodeId, String declaringTypeName, String methodSignature, int sourceLineNumber) {
        return withNode(nodeId, new SuperMethodCallNode(declaringTypeName, methodSignature, sourceLineNumber));
    }

    public APIUsageExampleBuilder withThrow(String nodeId, int sourceLineNumber) {
        return withNode(nodeId, new ThrowNode(sourceLineNumber));
    }

    public APIUsageExampleBuilder withCatch(String nodeId, String exceptionType, int sourceLineNumber) {
        return withNode(nodeId, new CatchNode(exceptionType, sourceLineNumber));
    }

    public APIUsageExampleBuilder withTypeCheck(String nodeId, String targetTypeName, int sourceLineNumber) {
        return withNode(nodeId, new TypeCheckNode(targetTypeName, sourceLineNumber));
    }

    public APIUsageExampleBuilder withUnaryOperator(String nodeId, String operator, int sourceLineNumber) {
        return withNode(nodeId, new UnaryOperatorNode(operator, sourceLineNumber));
    }

    // Data Nodes

    public APIUsageExampleBuilder withAnonymousClassMethod(String nodeId, String baseType, String methodSignature) {
        return withNode(nodeId, new AnonymousClassMethodNode(baseType, methodSignature));
    }

    public APIUsageExampleBuilder withAnonymousObject(String nodeId, String typeName) {
        return withNode(nodeId, new AnonymousObjectNode(typeName));
    }

    public APIUsageExampleBuilder withException(String nodeId, String typeName, String variableName) {
        return withNode(nodeId, new ExceptionNode(typeName, variableName));
    }

    public APIUsageExampleBuilder withLiteral(String nodeId, String typeName, String value) {
        return withNode(nodeId, new LiteralNode(typeName, value));
    }

    public APIUsageExampleBuilder withVariable(String nodeId, String dataTypeName, String variableName) {
        return withNode(nodeId, new VariableNode(dataTypeName, variableName));
    }

    public APIUsageExampleBuilder withVariable(String nodeId, String dataTypeName, String variableName, String paramIndex) {
        VariableNode node = new VariableNode(dataTypeName, variableName);
        node.setParamIndex(paramIndex);
        return withNode(nodeId, node);
    }

    public APIUsageExampleBuilder withConstant(String nodeId, String dataType, String dataName, String dataValue) {
        return withNode(nodeId, new ConstantNode(dataType, dataName, dataValue));
    }

    // Data-Flow Edges

    public APIUsageExampleBuilder withDefinitionEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new DefinitionEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withParameterEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ParameterEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withQualifierEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new QualifierEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withReceiverEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ReceiverEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    // Control-Flow Edges

    public APIUsageExampleBuilder withContainsEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ContainsEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withExceptionHandlingEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ExceptionHandlingEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withFinallyEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new FinallyEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withOrderEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new OrderEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withRepetitionEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new RepetitionEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withSelectionEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new SelectionEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withSynchronizationEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new SynchronizationEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    public APIUsageExampleBuilder withThrowEdge(String sourceNodeId, String targetNodeId) {
        return withEdge(new ThrowEdge(getNode(sourceNodeId), getNode(targetNodeId)));
    }

    //TODO (myCode) 利用现有Node和Edge来构建AUG
    public APIUsageExampleBuilder withCopyNode(String nodeId, Node node){
        int noSourceLine = -1;
        if(node instanceof ArrayAccessNode){
            ArrayAccessNode n = (ArrayAccessNode)node;
            withArrayAccess(nodeId, n.getDeclaringTypeName(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof ArrayAssignmentNode){
            ArrayAssignmentNode n = (ArrayAssignmentNode)node;
            withArrayAssignment(nodeId, n.getDeclaringTypeName(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof ArrayCreationNode){
            ArrayCreationNode n = (ArrayCreationNode)node;
            withArrayCreation(nodeId, n.getDeclaringTypeName(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof AssignmentNode){
            AssignmentNode n = (AssignmentNode)node;
            withAssignment(nodeId, n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof BreakNode){
            BreakNode n = (BreakNode)node;
            withBreak(nodeId, n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof CastNode){
            CastNode n = (CastNode)node;
            withCast(nodeId, n.getTargetType(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof SuperConstructorCallNode){
            SuperConstructorCallNode n = (SuperConstructorCallNode)node;
            withSuperConstructorCall(nodeId, n.getDeclaringTypeName(), n.getMethodSignature(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof ConstructorCallNode){
            ConstructorCallNode n = (ConstructorCallNode)node;
            withConstructorCall(nodeId, n.getDeclaringTypeName(), n.getMethodSignature(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof ContinueNode){
            ContinueNode n = (ContinueNode)node;
            withContinue(nodeId, n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof NullCheckNode){
            NullCheckNode n = (NullCheckNode)node;
            withNullCheck(nodeId, n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof InfixOperatorNode){
            InfixOperatorNode n = (InfixOperatorNode)node;
            withInfixOperator(nodeId, n.getOperator(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof SuperMethodCallNode){
            SuperMethodCallNode n = (SuperMethodCallNode)node;
            withSuperMethodCall(nodeId, n.getDeclaringTypeName(), n.getMethodSignature(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof MethodCallNode){
            MethodCallNode n = (MethodCallNode)node;
            withMethodCall(nodeId, n.getDeclaringTypeName(), n.getMethodSignature(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof ReturnNode){
            ReturnNode n = (ReturnNode)node;
            withReturn(nodeId, n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof ThrowNode){
            ThrowNode n = (ThrowNode)node;
            withThrow(nodeId, n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof CatchNode){
            CatchNode n = (CatchNode)node;
            withCatch(nodeId, n.getExceptionType(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof TypeCheckNode){
            TypeCheckNode n = (TypeCheckNode)node;
            withTypeCheck(nodeId, n.getTargetTypeName(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof UnaryOperatorNode){
            UnaryOperatorNode n = (UnaryOperatorNode)node;
            withUnaryOperator(nodeId, n.getOperator(), n.getSourceLineNumber().orElse(noSourceLine));
        }else if(node instanceof AnonymousClassMethodNode){
            AnonymousClassMethodNode n = (AnonymousClassMethodNode)node;
            withAnonymousClassMethod(nodeId, n.getType(), n.getName());
        }else if(node instanceof AnonymousObjectNode){
            AnonymousObjectNode n = (AnonymousObjectNode)node;
            withAnonymousObject(nodeId, n.getType());
        }else if(node instanceof ExceptionNode){
            ExceptionNode n = (ExceptionNode)node;
            withException(nodeId, n.getType(), n.getName());
        }else if(node instanceof LiteralNode){
            LiteralNode n = (LiteralNode)node;
            withLiteral(nodeId, n.getType(), n.getValue());
        }else if(node instanceof VariableNode){
            VariableNode n = (VariableNode)node;
            withVariable(nodeId, n.getType(), n.getName(), n.getParamIndex());
        }else if(node instanceof ConstantNode){
            ConstantNode n = (ConstantNode)node;
            withConstant(nodeId, n.getType(), n.getName(), n.getValue());
        }
        return this;
    }

    public APIUsageExampleBuilder withCopyEdge(Edge edge, String sourceId, String targetId){
        if(edge instanceof DefinitionEdge){
            withDefinitionEdge(sourceId, targetId);
        }else if(edge instanceof ParameterEdge){
            withParameterEdge(sourceId, targetId);
        }else if(edge instanceof QualifierEdge){
            withQualifierEdge(sourceId, targetId);
        }else if(edge instanceof ReceiverEdge){
            withReceiverEdge(sourceId, targetId);
        }else if(edge instanceof ContainsEdge){
            withContainsEdge(sourceId, targetId);
        }else if(edge instanceof ExceptionHandlingEdge){
            withExceptionHandlingEdge(sourceId, targetId);
        }else if(edge instanceof FinallyEdge){
            withFinallyEdge(sourceId, targetId);
        }else if(edge instanceof OrderEdge){
            withOrderEdge(sourceId, targetId);
        }else if(edge instanceof RepetitionEdge){
            withRepetitionEdge(sourceId, targetId);
        }else if(edge instanceof SelectionEdge){
            withSelectionEdge(sourceId, targetId);
        }else if(edge instanceof SynchronizationEdge){
            withSynchronizationEdge(sourceId, targetId);
        }else if(edge instanceof ThrowEdge){
            withThrowEdge(sourceId, targetId);
        }
        return this;
    }

    // helpers

    public APIUsageExampleBuilder withNode(String nodeId, Node node) {
        nodeMap.put(nodeId, node);
        return this;
    }

    public APIUsageExampleBuilder withEdge(Edge edge) {
        edges.add(edge);
        return this;
    }

    private Node getNode(String nodeId) {
        if (!nodeMap.containsKey(nodeId)) {
            throw new IllegalArgumentException("node with id '" + nodeId + "' does not exist");
        }
        return nodeMap.get(nodeId);
    }

    public APIUsageExample build() {
        APIUsageExample aug = new APIUsageExample(location);
        aug = (APIUsageExample) buildGraph(aug);
        return aug;
    }

    //TODO (myCode)
    private APIUsageGraph buildGraph(APIUsageGraph aug){
        for (Node node : nodeMap.values()) {
            aug.addVertex(node);
            node.setGraph(aug);
        }
        for (Edge edge : edges) {
            try {
                aug.addEdge(edge.getSource(), edge.getTarget(), edge);
            }catch (IllegalArgumentException e){
                System.out.printf("%s : %s at %s", e.getClass().getName(), e.getMessage(), String.join("|", aug.getAPIs()));
            }
        }
        return aug;
    }

    //TODO (myCode)
    public APIUsageExample buildExample(Location location) {
        APIUsageExample example = new APIUsageExample(location);
        example = (APIUsageExample) buildGraph(example);
        return example;
    }

    //TODO (myCode)
    public APIUsagePattern buildPattern(int support, Set<Location> exampleLocations){
        APIUsagePattern pattern = new APIUsagePattern(support, new HashSet<>(exampleLocations));
        pattern = (APIUsagePattern) buildGraph(pattern);
        return pattern;
    }
}
