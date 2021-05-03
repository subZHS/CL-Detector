package de.tu_darmstadt.stg.sourcerule;

import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.egroum.aug.EGroumGraph;
import edu.iastate.cs.egroum.aug.UsageExamplePredicate;
import edu.iastate.cs.egroum.utils.JavaASTUtil;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SpecificMethodPredicate implements UsageExamplePredicate {

    private final Map<String, Set<String>> apiMethodLocation;
    private String curFilePath = null;

    public static SpecificMethodPredicate examplesOf(Map<String, Set<String>> apiMethodLocation) {
        return new SpecificMethodPredicate(apiMethodLocation);
    }

    private SpecificMethodPredicate(Map<String, Set<String>> apiMethodLocation) {
        if(apiMethodLocation==null){
            apiMethodLocation = new LinkedHashMap<>();
        }
        this.apiMethodLocation = apiMethodLocation;
    }

    @Override
    public boolean matches(String sourceFilePath, CompilationUnit cu) {
        if(getByEndWith(apiMethodLocation, sourceFilePath).isEmpty()){
            return false;
        }
        this.curFilePath = sourceFilePath;
        return true;
    }

    public static Set<String> getByEndWith(Map<String, Set<String>> apiMethodLocation, String sourceFilePath){
        for(String classRelativePath: apiMethodLocation.keySet()){
            if(sourceFilePath.endsWith(classRelativePath)){
                return apiMethodLocation.get(classRelativePath);
            }
        }
        return new HashSet<>();
    }

    @Override
    public boolean matches(MethodDeclaration methodDeclaration) {
        if(curFilePath==null){
            return false;
        }
        if(getByEndWith(apiMethodLocation, curFilePath).isEmpty()){
            return false;
        }
        String signature = AUGHandler.getMethodSignature(methodDeclaration);
        if(!getByEndWith(apiMethodLocation, curFilePath).contains(signature)){
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(EGroumGraph eGroumGraph) {
        return true;
    }


}
