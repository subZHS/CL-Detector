package de.tu_darmstadt.stg.mudetect.aug.model.actions;

import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

public class SuperConstructorCallNode extends ConstructorCallNode {
    public SuperConstructorCallNode(String superTypeName) {
        super(superTypeName);
    }

    public SuperConstructorCallNode(String superTypeName, int sourceLineNumber) {
        super(superTypeName, sourceLineNumber);
    }

    public SuperConstructorCallNode(String superTypeName, String methodSignature, int sourceLineNumber) {
        super(superTypeName, methodSignature, sourceLineNumber);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
