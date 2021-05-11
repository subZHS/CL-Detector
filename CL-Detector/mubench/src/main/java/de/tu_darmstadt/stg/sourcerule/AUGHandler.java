package de.tu_darmstadt.stg.sourcerule;

import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.Node;
import de.tu_darmstadt.stg.mudetect.aug.model.dot.AUGDotExporter;
import de.tu_darmstadt.stg.mudetect.aug.model.dot.AUGEdgeAttributeProvider;
import de.tu_darmstadt.stg.mudetect.aug.model.dot.AUGNodeAttributeProvider;
import de.tu_darmstadt.stg.mudetect.aug.visitors.AUGLabelProvider;
import de.tu_darmstadt.stg.mudetect.aug.visitors.BaseAUGLabelProvider;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.egroum.utils.JavaASTUtil;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class AUGHandler {

    public static void toDotFile(APIUsageExample example, String apiClass, String method, String fileName){
        AUGDotExporter exporter = new AUGDotExporter(new APISourceAUGLabelProvider(),
                new AUGNodeAttributeProvider(),
                new AUGEdgeAttributeProvider());
        try {
//            File file = new File(String.join(File.separator, Arrays.asList("dotGraph", apiClass, method, fileName)));
//            exporter.toPNGFile(example, file);
//            exporter.toDotFile(example, file);
//            Runtime rt = Runtime.getRuntime();
//
//            String cd = "cd '" + file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("/")) + "'";
//            String dot = "dot " + fileName +".dot" + " -T png -o " + fileName + ".png";
//            Process p = rt.exec(new String[]{"bash","-c", String.join(";", new String[]{cd, dot})});
//            p.waitFor();
//            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Node> findNodeInAUG(APIUsageExample aug, List<String> nodeLabels, List<String> fuzzyWords){
        List<Node> nodeIdList = new ArrayList<>();
        AUGLabelProvider labelProvider = new BaseAUGLabelProvider();
        for(Node node: aug.vertexSet()){
            String label = labelProvider.getLabel(node);
            if((nodeLabels!=null && nodeLabels.contains(label)) ||
                    (fuzzyWords!= null && strContain(label, fuzzyWords))){
                nodeIdList.add(node);
            }
        }
        return nodeIdList;
    }

    private static boolean strContain(String str, List<String> fuzzywords){
        for(String fuzzyword : fuzzywords) {
            if (str.contains(fuzzyword)) {
                return true;
            }
        }
        return false;
    }

    public static String getMethodSignature(MethodDeclaration methodDeclaration){
        return AUGBuilder.getMethodSignature(JavaASTUtil.buildSignature(methodDeclaration));
    }

    public static String getSimpleName(String apiClass){
        return apiClass.substring(apiClass.lastIndexOf(".")+1);
    }

}
