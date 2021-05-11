package de.tu_darmstadt.stg.sourcerule.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TypeVisitor extends ASTVisitor {

    private String typeName;

    public TypeVisitor(String typeName){
        this.typeName = typeName;
    }

    @Override
    public boolean visit(TypeDeclaration type){
        if(!type.getName().getIdentifier().equals(typeName)){
            type.delete();
            return false;
        }
        return true;
    }
}
