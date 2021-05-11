package de.tu_darmstadt.stg.sourcerule.visitor;

import de.tu_darmstadt.stg.sourcerule.SourceRule;
import org.eclipse.jdt.core.dom.*;

public class StatementVisitor extends ASTVisitor {

    private SourceRule sourceRule;
    private CompilationUnit cu;

    public StatementVisitor(CompilationUnit cu, SourceRule sourceRule){
        this.cu = cu;
        this.sourceRule = sourceRule;
    }

    @Override
    public boolean preVisit2(ASTNode node){
        if(node instanceof Statement){
            int startLineNumber = cu.getLineNumber(node.getStartPosition());
            int endLineNumber = cu.getLineNumber(node.getStartPosition()+node.getLength()-1);
            if(startLineNumber >= sourceRule.getStartLine() && endLineNumber <= sourceRule.getEndLine()){
                return false;
            }
            if(endLineNumber < sourceRule.getStartLine() || startLineNumber > sourceRule.getEndLine()){
                node.delete();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean visit(MethodDeclaration method){
        if(method.getJavadoc()!= null) {
            method.getJavadoc().delete();
        }
        return true;
    }

}
