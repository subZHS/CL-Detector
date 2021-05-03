package de.tu_darmstadt.stg.sourcerule.visitor;

import de.tu_darmstadt.stg.sourcerule.AUGHandler;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodDeclarationVisitor extends ASTVisitor {

    private String methodSig;

    public MethodDeclarationVisitor(String sig){
        this.methodSig = sig;
    }

    @Override
    public boolean visit(MethodDeclaration method){
        String sig = AUGHandler.getMethodSignature(method);
        //如果methodSig为null，则删除所有方法
        if(methodSig == null || !methodSig.equals(sig)){
            method.delete();
            return false;
        }
        return true;
    }
}
