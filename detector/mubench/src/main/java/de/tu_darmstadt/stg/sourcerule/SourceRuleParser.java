package de.tu_darmstadt.stg.sourcerule;

import de.tu_darmstadt.stg.mubench.API;
import de.tu_darmstadt.stg.mubench.CrossProjectStrategy;
import de.tu_darmstadt.stg.mubench.DefaultAUGConfiguration;
import de.tu_darmstadt.stg.mudetect.aug.builder.APIUsageExampleBuilder;
import de.tu_darmstadt.stg.mudetect.aug.model.*;
import de.tu_darmstadt.stg.mudetect.aug.model.actions.*;
import de.tu_darmstadt.stg.mudetect.aug.model.controlflow.*;
import de.tu_darmstadt.stg.mudetect.aug.model.data.VariableNode;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.ParameterEdge;
import de.tu_darmstadt.stg.mudetect.aug.model.dataflow.ReceiverEdge;
import de.tu_darmstadt.stg.mudetect.aug.visitors.AUGLabelProvider;
import de.tu_darmstadt.stg.sourcerule.visitor.StatementVisitor;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.egroum.aug.AUGConfiguration;
import edu.iastate.cs.egroum.aug.EGroumBuilder;
import edu.iastate.cs.egroum.aug.EGroumNode;
import edu.iastate.cs.egroum.utils.FileIO;
import edu.iastate.cs.egroum.utils.JavaASTUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.tu_darmstadt.stg.sourcerule.SourceRule.SourceRuleType;

public class SourceRuleParser {

//    public static String sourceRuleExcelPath = "/Users/hsz/zhs/sjtu/APIMisuse/dataset/libraryRule/rule_File.xlsx";
    public static String sourceRuleCSVPath = CrossProjectStrategy.getSourceRuleFilePath().toString();
    public static String libRootDir = "/Users/hsz/zhs/sjtu/APIMisuse/dataset/libraryRule/libSrc";
    public Map<String, String> apiLocationMap = new HashMap<>();
    private static SoftReference<Map<String, SourceRule>> bufferSourceRuleMap = new SoftReference<>(new HashMap<>());

    public static void main(String[] args) throws IOException {
//        obtainSourceRules(new HashSet<>(Arrays.asList("android.view.View", "java.security.KeyStore", "java.net.URL")));
//        obtainSourceRules(null);
        Map<SourceRuleType, Integer> ruleTypeNumMap = calculateRuleNumForEachType();
        System.out.println("ruleTotalNum: " + ruleTypeNumMap.values().stream().mapToInt(i->i).sum());
        System.out.println("ruleTypeNumMap: " + ruleTypeNumMap);
    }

    public static Map<SourceRuleType, Integer> calculateRuleNumForEachType() throws IOException{
        SourceRuleParser sourceRuleParser = new SourceRuleParser();
        Map<String, SourceRule> sourceRuleMap = sourceRuleParser.readRuleCSV(null, sourceRuleCSVPath);
        Map<SourceRuleType, Integer> ruleTypeNumMap = new HashMap<>();
        for(String ruleId : sourceRuleMap.keySet()){
            SourceRuleType type = sourceRuleMap.get(ruleId).getType();
            ruleTypeNumMap.putIfAbsent(type, 0);
            ruleTypeNumMap.put(type, ruleTypeNumMap.get(type)+1);
        }
        return ruleTypeNumMap;
    }

    public static List<SourceRule> obtainSourceRules(Set<String> apis) throws IOException {
        SourceRuleParser sourceRuleParser = new SourceRuleParser();
        Map<String, SourceRule> sourceRuleMap = sourceRuleParser.readRuleCSV(apis, sourceRuleCSVPath);
        System.out.println("total rule line num from ruleFile: " + sourceRuleMap.size());
        List<SourceRule> ruleList = new ArrayList<>();
        int index=0;
        for(SourceRule rule : sourceRuleMap.values()){
//            if(!rule.getType().equals(SourceRuleType.Order)){
//                continue;
//            }
            System.out.println("ruleIndex: " + index);
            index++;

            SourceRule bufferRule = bufferSourceRuleMap.get().get(rule.getId());
            if(bufferRule!=null){
                rule = bufferRule;
            }else {
                List<APIUsageExample> augList = sourceRuleParser.parseAUG(rule);
                if (augList == null || augList.isEmpty()) {
                    continue;
                }
                rule.setAugList(augList);
            }
            ruleList.add(rule);
//            break;
        }
        bufferSourceRuleMap.get().putAll(sourceRuleMap);

        System.out.println("available rule size: " + ruleList.size());
        List<SourceRule> reducedRuleList = groupExceptionAndConditionAUG(ruleList);
        System.out.println("after grouping rule size: " + reducedRuleList.size());
        return reducedRuleList;

//        SourceRule sourceRule = new SourceRule();
//        sourceRule.setApiClass("de.tu_darmstadt.stg.sourcerule.Example");
//        sourceRule.setApiMethod("hello(ArrayList, int)");
//        sourceRule.setFileLocation("/Users/hsz/projects/MUDetect/mubench/src/main/java/de/tu_darmstadt/stg/sourcerule/Example.java");
//        sourceRule.setType(SourceRuleType.Condition);
//        sourceRule.setException("NullPointerException");
//        sourceRule.setExceptionCondition(null);
//        sourceRule.setCondition("arg0 > 0 && arg0 < 10");
////        sourceRule.setSequence(Arrays.asList("this", "PrintStream.println(String)"));
//        sourceRule.setStartLine(0);
//        sourceRule.setEndLine(16);
//        List<APIUsageExample> augList = sourceRuleParser.parseAUG(sourceRule);
//        sourceRule.setAugList(augList);
//        return Arrays.asList(sourceRule);
    }

    public List<APIUsageExample> parseAUG(SourceRule sourceRule){
        List<APIUsageExample> augList = new ArrayList<>();
//        Map<String, Set<String>> methods = new LinkedHashMap<>();
//        methods.put(sourceRule.getFileLocation(), new HashSet<>(Arrays.asList(sourceRule.getApiMethod())));
//        Collection<APIUsageExample> augs  = new AUGBuilder(new AUGConfiguration(){
//            {this.usageExamplePredicate = SpecificMethodPredicate.examplesOf(methods);}
//        }).build(methods.keySet().toArray(new String[0]), new String[0]);
//        for(APIUsageExample aug : augs){
//            AUGHandler.toDotFile(aug, sourceRule.getApiClass(), aug.getLocation().getMethodSignature(), "AUG");
//        }

//        String source = String.format("package %d; " +
//                "%d" +
//                "class %d {" +
//                "}");
        //先定位到对应api方法
        File apiFile = new File(sourceRule.getFileLocation());
        String sourceCode = FileIO.readStringFromFile(apiFile.getAbsolutePath());
        CompilationUnit cu = parseAST(sourceCode, sourceRule);
        MethodDeclaration method = extractMethod(cu, sourceRule);
        if(method == null){
            System.out.println("parseAUG: can't find method " + sourceRule.getApiMethod() + " from " + sourceRule.getFileLocation());
            return null;
        }
        //过滤出跟API规则相关的statements
        StatementVisitor statementVisitor = new StatementVisitor(cu, sourceRule);
        method.accept(statementVisitor);

        if(sourceRule.getType().equals(SourceRuleType.Exception)){
            /*
            if(sourceRule.getExceptionCondition() != null){//生成Exception相关前置条件规则的AUG
                  //简单修改方法源码以便构建使用API的客户代码：添加对该API的调用
//                String methodCall = buildMethodCall(method);
//                String mSource = method.toString();
//                String mNewSource = mSource.substring(0, mSource.lastIndexOf("}")) + methodCall + "\n}\n";
//                MethodDeclaration newMethod = extractNewMethod(cu, method, mNewSource, sourceRule);
//                //对处理过的方法构建AUG
//                APIUsageExample aug = buildAPIUsageGraph(newMethod, sourceRule);
//                //修补AUG
//                List<String> deleteNodeLabels = Arrays.asList("<throw>", AUGHandler.getSimpleName(sourceRule.getException()));
//                List<Node> deleteNodes = AUGHandler.findNodeInAUG(aug, deleteNodeLabels, null);
//                //添加Exception的def节点
//                for(Node node : new ArrayList<>(deleteNodes)){
//                    for (Edge e : aug.incomingEdgesOf(node)) {
//                        if(e instanceof DefinitionEdge){
//                            deleteNodes.add(e.getSource());
//                        }
//                    }
//                }
//                aug.removeAllVertices(deleteNodes);
                method.getBody().statements().clear();
                String methodCall = buildMethodCall(method);
                String statement = String.format("if(%s){ \n%s \n}\n", buildCondition(method, sourceRule.getExceptionCondition().getCondition()), methodCall);

                String mSource = method.toString();
                String mNewSource = mSource.substring(0, mSource.lastIndexOf("}")) + statement + "\n}\n";
                MethodDeclaration newMethod = extractNewMethod(cu, method, mNewSource, sourceRule);

                APIUsageExample aug = buildAPIUsageGraph(newMethod, sourceRule);
                augList.add(aug);
                AUGHandler.toDotFile(aug, sourceRule.getApiClass(), aug.getLocation().getMethodSignature(), "exCon");
            }
             */

            // 生成catch Exception规则的AUG
            if(method.getBody()!=null){
                method.getBody().statements().clear();
            }else {
                method.setBody(method.getAST().newBlock());
            }
            String mSource = method.toString();
            String mNewSource = mSource.substring(0, mSource.lastIndexOf("}")) +
                    String.format("try {\n %s\n }catch (%s e){\n}", buildMethodCall(method), sourceRule.getException())+ "\n}\n";
            MethodDeclaration newMethod = extractNewMethod(cu, method, mNewSource, sourceRule);
            APIUsageExample aug = buildAPIUsageGraph(newMethod, sourceRule);
            augList.add(aug);
            AUGHandler.toDotFile(aug, sourceRule.getApiClass(), aug.getLocation().getMethodSignature(), "exception");

        }else if(sourceRule.getType().equals(SourceRuleType.Condition)){
            if(method.getBody()!=null){
                method.getBody().statements().clear();
            }else {
                method.setBody(method.getAST().newBlock());
            }
            String methodCall = buildMethodCall(method);
            String condition = buildCondition(method, sourceRule.getCondition());
            if (condition==null){
                System.out.println("parseAUG: can't build condtion at " + sourceRule.getApiMethod() + " from " + sourceRule.getFileLocation());
                return null;
            }
            String statement = String.format("if(%s){ \n%s \n}\n", condition, methodCall);

            String mSource = method.toString();
            String mNewSource = mSource.substring(0, mSource.lastIndexOf("}")) + statement + "\n}\n";
            MethodDeclaration newMethod = extractNewMethod(cu, method, mNewSource, sourceRule);

            APIUsageExample aug = buildAPIUsageGraph(newMethod, sourceRule);
            augList.add(aug);
            AUGHandler.toDotFile(aug, sourceRule.getApiClass(), aug.getLocation().getMethodSignature(), "condition");

        }else if(sourceRule.getType().equals(SourceRuleType.Order)){
            List<String> apiOrderList = sourceRule.getSequence();
            int thisIndex = apiOrderList.indexOf("this");
            String thisFullMethodSig = sourceRule.getApiClass() + "." + sourceRule.getApiMethod();
            String a = null, b = null;
            for(int i = 0; i<thisIndex; i++){
                a = apiOrderList.get(i);
                if(!a.contains(".")){
                    a = sourceRule.getApiClass() + "." + a;
                }
                b = thisFullMethodSig;
                APIUsageExample aug = buildOrderAUG(a, b, sourceRule);
                augList.add(aug);
            }
            for(int i = thisIndex+1; i< apiOrderList.size();i++){
                a = thisFullMethodSig;
                b = apiOrderList.get(i);
                if(!b.contains(".")){
                    b = sourceRule.getApiClass() + "." + b;
                }
                APIUsageExample aug = buildOrderAUG(a, b, sourceRule);
                augList.add(aug);
//                AUGHandler.toDotFile(aug, sourceRule.getApiClass(), aug.getLocation().getMethodSignature(), "order"+i);
            }
        }

        return augList;
    }

    private APIUsageExample buildOrderAUG(String aMethodSig, String bMethodSig, SourceRule sourceRule){
        APIUsageExampleBuilder builder = APIUsageExampleBuilder.buildAUG();
        String aNodeId = EGroumNode.newNodeId();
        if(isConstructor(aMethodSig)){
            builder.withConstructorCall(aNodeId, getTypeName(aMethodSig), getPureMethodSig(aMethodSig), -1);
        }else{
            builder.withMethodCall(aNodeId, getTypeName(aMethodSig), getPureMethodSig(aMethodSig), -1);
        }

        String bNodeId = EGroumNode.newNodeId();
        if(isConstructor(bMethodSig)){
            builder.withConstructorCall(bNodeId, getTypeName(bMethodSig), getPureMethodSig(bMethodSig), -1);
        }else{
            builder.withMethodCall(bNodeId, getTypeName(bMethodSig), getPureMethodSig(bMethodSig), -1);
        }

        builder.withOrderEdge(aNodeId, bNodeId);
        return builder.buildExample(new Location(sourceRule.getFileLocation(), sourceRule.getFileLocation(), sourceRule.getApiMethod()));
    }

    public static List<SourceRule> groupExceptionAndConditionAUG(List<SourceRule> ruleList){
        ruleList = ruleList.stream().filter(sourceRule -> !CollectionUtils.isEmpty(sourceRule.getAugList())).collect(Collectors.toList());
        List<SourceRule> resultRuleList = new ArrayList<>();

        //将所有的condition组合，用组合aug后的rule替换被组合的ruleList
        List<SourceRule> reducedConRuleList = new ArrayList<>();

        Map<String, List<SourceRule>> fullSigConditionRuleMap =
                ruleList.stream().filter(rule -> rule.getType().equals(SourceRuleType.Condition))
                        .collect(Collectors.groupingBy(rule -> rule.getApiClass() + "." + rule.getApiMethod()));

        Map<String, List<SourceRule>> absoluteFullSigExceptionRuleMap = ruleList.stream()
                .filter(rule -> rule.getType().equals(SourceRuleType.Exception) && rule.getExceptionCondition()==null)
                .collect(Collectors.groupingBy(rule -> rule.getApiClass() + "." + rule.getApiMethod()));

        for(String fullSig : fullSigConditionRuleMap.keySet()){
            List<SourceRule> rules = fullSigConditionRuleMap.getOrDefault(fullSig, new ArrayList<>());
            //加上无对应excConditionId的exception规则
            rules.addAll(absoluteFullSigExceptionRuleMap.get(fullSig)==null? new ArrayList<>() : absoluteFullSigExceptionRuleMap.get(fullSig));
            if(rules.size()<=0){
                continue;
            }
            SourceRule reducedRule = new SourceRule(rules.get(0));
            reducedRule.setExceptionCondition(null);
            for(int i=1;i<rules.size();i++){
                APIUsageExample aug = mergeAUG(reducedRule.getAugList().get(0), rules.get(i).getAugList().get(0));
                reducedRule.setAugList(new ArrayList<>());
                reducedRule.getAugList().add(aug);
            }
            AUGHandler.toDotFile(reducedRule.getAugList().get(0), reducedRule.getApiClass(), reducedRule.getApiMethod(), "reducedCon");
            reducedConRuleList.add(reducedRule);
        }

        //考虑单个Exception和所有Exception，将Exception对应的exceptionCondition从condition规则中移除
        List<SourceRule> reducedExceptionRuleList = new ArrayList<>();
        Map<String, List<SourceRule>> fullSigExceptionRuleMap =
                ruleList.stream().filter(rule -> rule.getType().equals(SourceRuleType.Exception))
                        .collect(Collectors.groupingBy(rule -> rule.getApiClass() + "." + rule.getApiMethod()));
        for(String fullSig : fullSigExceptionRuleMap.keySet()){
            List<SourceRule> exceptionRules = fullSigExceptionRuleMap.get(fullSig);
            Map<String, List<SourceRule>> exceptionRuleMap = exceptionRules.stream().collect(Collectors.groupingBy(SourceRule::getException));
            //单个Exception
            for(String exception : exceptionRuleMap.keySet()){
                SourceRule reducedRule = new SourceRule(exceptionRuleMap.get(exception).get(0));
                reducedRule.setExceptionCondition(null);
                List<String> exceptionConIds = exceptionRuleMap.get(exception).stream()
                        .filter(rule -> rule.getExceptionCondition()!=null)
                        .map(rule->rule.getExceptionCondition().getId()).collect(Collectors.toList());
                List<SourceRule> remainCondRuleList = fullSigConditionRuleMap.getOrDefault(fullSig, new ArrayList<>()).stream()
                        .filter(rule -> !exceptionConIds.contains(rule.getId())).collect(Collectors.toList());
                for(int i=0;i<remainCondRuleList.size();i++){
                    APIUsageExample aug = mergeAUG(reducedRule.getAugList().get(0), remainCondRuleList.get(i).getAugList().get(0));
                    reducedRule.setAugList(new ArrayList<>());
                    reducedRule.getAugList().add(aug);
                }
                AUGHandler.toDotFile(reducedRule.getAugList().get(0), reducedRule.getApiClass(), reducedRule.getApiMethod(), "singleExc-"+exception);
                reducedExceptionRuleList.add(reducedRule);
            }
            //所有Exception
            exceptionRules.clear();
            exceptionRuleMap.forEach((exception, rules) -> exceptionRules.add(rules.get(0)));
            if(exceptionRules.size()<2){
                continue;
            }
            SourceRule reducedRule = new SourceRule(exceptionRules.get(0));
            reducedRule.setException(null);
            reducedRule.setExceptionCondition(null);
            for(int i =1 ;i<exceptionRules.size();i++){
                APIUsageExample aug = mergeAUG(reducedRule.getAugList().get(0), exceptionRules.get(i).getAugList().get(0));
                reducedRule.setAugList(new ArrayList<>());
                reducedRule.getAugList().add(aug);
            }
            List<String> exceptionConIds = exceptionRules.stream()
                    .filter(rule -> rule.getExceptionCondition()!=null)
                    .map(rule -> rule.getExceptionCondition().getId()).collect(Collectors.toList());
            List<SourceRule> remainCondRuleList = fullSigConditionRuleMap.getOrDefault(fullSig, new ArrayList<>()).stream()
                    .filter(rule -> !exceptionConIds.contains(rule.getId())).collect(Collectors.toList());
            for(int i=0;i<remainCondRuleList.size();i++){
                APIUsageExample aug = mergeAUG(reducedRule.getAugList().get(0), remainCondRuleList.get(i).getAugList().get(0));
                reducedRule.setAugList(new ArrayList<>());
                reducedRule.getAugList().add(aug);
            }
            AUGHandler.toDotFile(reducedRule.getAugList().get(0), reducedRule.getApiClass(), reducedRule.getApiMethod(), "allExc");
            reducedExceptionRuleList.add(reducedRule);
        }

        resultRuleList.addAll(reducedConRuleList);
        resultRuleList.addAll(reducedExceptionRuleList);
        resultRuleList.addAll(ruleList.stream().filter(rule -> rule.getType().equals(SourceRuleType.Order)).collect(Collectors.toList()));
        return resultRuleList;
    }

    //该合并方法适用范围很有限，会合并label相同的节点，如果某个AUG出现多个相同label的节点会出错
    private static APIUsageExample mergeAUG(APIUsageExample aug1, APIUsageExample aug2){
        APIUsageExampleBuilder builder = mergeAUGBuilder(aug1, aug2);
        return builder.buildExample(new Location(aug1.getLocation().getProjectName(), aug1.getLocation().getFilePath(), aug1.getLocation().getMethodSignature()));
    }

    public static APIUsageExampleBuilder mergeAUGBuilder(APIUsageGraph aug1, APIUsageGraph aug2){
        AUGLabelProvider labelProvider = new APISourceAUGLabelProvider();
        APIUsageExampleBuilder builder = APIUsageExampleBuilder.buildAUG();

        Map<String, Node> nodeLabelMap = new LinkedHashMap<>();
        List<Node> allNodes = new ArrayList<>(aug1.vertexSet());
        allNodes.addAll(aug2.vertexSet());
        for(Node node : allNodes){
            nodeLabelMap.put(labelProvider.getLabel(node), node);
        }

        Map<String, String> nodeLabelIdMap = new LinkedHashMap<>();
        for(Map.Entry<String, Node> entry: nodeLabelMap.entrySet()){
            String nodeId = EGroumNode.newNodeId();
            builder.withCopyNode(nodeId, entry.getValue());
            nodeLabelIdMap.put(entry.getKey(), nodeId);
        }

        Map<String, Edge> allEdges = new HashMap<>();
        aug1.edgeSet().forEach(edge->allEdges.put(labelProvider.getLabel(edge), edge));
        aug2.edgeSet().forEach(edge->allEdges.put(labelProvider.getLabel(edge), edge));
        for(Edge edge : allEdges.values()){
            String sourceId = nodeLabelIdMap.get(labelProvider.getLabel(edge.getSource()));
            String targetId = nodeLabelIdMap.get(labelProvider.getLabel(edge.getTarget()));
            builder.withCopyEdge(edge, sourceId, targetId);
        }
        return builder;
    }

    public APIUsageExample buildAPIUsageGraph(MethodDeclaration method, SourceRule sourceRule){
        AUGConfiguration configuration = new DefaultAUGConfiguration(){{ removeImplementationCode = 0; }};
        AUGBuilder augBuilder = new AUGBuilder(configuration);
        EGroumBuilder eGroumBuilder = new EGroumBuilder(configuration);
        eGroumBuilder.buildStandardJars();
        File apiFile = new File(sourceRule.getFileLocation());
        eGroumBuilder.buildHierarchy(apiFile);
        if(!StringUtils.isEmpty(sourceRule.getLibraryJarPath(libRootDir))) {
            File libJarDir = new File(sourceRule.getLibraryJarPath(libRootDir));
            if (libJarDir.exists()) {
                eGroumBuilder.buildHierarchy(libJarDir);
            }
        }

        String apiSimpleName = AUGHandler.getSimpleName(sourceRule.getApiClass());
        APIUsageExample aug = augBuilder.toAUG(eGroumBuilder.buildGroum(method, sourceRule.getFileLocation(), apiSimpleName + "."));
        aug = pruneAUG(aug);

        //对aug的小修改：对API方法调用涉及的参数标明参数序号
        for(Node node : aug.vertexSet()){
            if(node instanceof MethodCallNode){
                MethodCallNode methodCallNode = (MethodCallNode)node;
                for(Edge paramEdge : aug.incomingEdgesOf(methodCallNode)){
                    if(paramEdge instanceof ParameterEdge && paramEdge.getSource() instanceof VariableNode){
                        VariableNode paramNode = (VariableNode) paramEdge.getSource();
                        String paramIndex = paramNameToIndex(method, paramNode.getName());
                        if(paramIndex!=null){
                            paramNode.setParamIndex(methodCallNode.getMethodName() + "(" + paramIndex + ")");
                        }
                    }
                }
            }
        }
        return aug;
    }

    //规则AUG剪枝
    private APIUsageExample pruneAUG(APIUsageExample aug){
        for(Node node : nodeListToIgnore(aug)){
            aug.removeVertex(node);
        }
        //去掉操作符之间的sel边
        for(Edge edge: new ArrayList<>(aug.edgeSet())){
            if(edge instanceof SelectionEdge &&
                    edge.getSource() instanceof OperatorNode && edge.getTarget() instanceof OperatorNode){
                aug.removeEdge(edge);
            }
        }

        return aug;
    }

    public static List<Node> nodeListToIgnore(APIUsageGraph aug){
        List<Node> nodeList = new ArrayList<>();
        //只发出recv边的对象节点可以去掉，只发出param边的参数节点也可以去掉
        for(Node node : new ArrayList<>(aug.vertexSet())){
            if(node instanceof DataNode && aug.incomingEdgesOf(node).size() ==0 && aug.outgoingEdgesOf(node).size() > 0){
                Set<String> types = aug.outgoingEdgesOf(node).stream()
                        .map(e -> e.getClass().getName() + " " + e.getTarget().getClass().getName()).collect(Collectors.toSet());
                if(types.size() <=1) {
                    Edge edge = aug.outgoingEdgesOf(node).iterator().next();
                    if (edge.getTarget() instanceof MethodCallNode)
                        if (edge instanceof ReceiverEdge || edge instanceof ParameterEdge) {
                            nodeList.add(node);
                        }
                }
            }
        }
        return nodeList;
    }

    public MethodDeclaration extractMethod(CompilationUnit cu, SourceRule sourceRule){
        MethodDeclaration result = null;
        String apiSimpleName = AUGHandler.getSimpleName(sourceRule.getApiClass());
        //删除除了目标method外的其他type和method
//        TypeVisitor typeVisitor = new TypeVisitor(apiSimpleName);
//        cu.accept(typeVisitor);
//        MethodDeclarationVisitor methodDeclarationVisitor = new MethodDeclarationVisitor(sourceRule.getApiMethod());
//        cu.accept(methodDeclarationVisitor);
        for(AbstractTypeDeclaration at : (List<AbstractTypeDeclaration>)cu.types()){
            if (!(at instanceof TypeDeclaration) || !at.getName().getIdentifier().equals(apiSimpleName)){
                continue;
            }
            TypeDeclaration type = (TypeDeclaration) at;
            for (MethodDeclaration method : type.getMethods()){
                String sig = AUGHandler.getMethodSignature(method);
                if(!sourceRule.getApiMethod().equals(sig)){
                    continue;
                }
                result = method;
            }
        }
        return result;
    }

    //从cu中删除原始的method，并添加新方法的源码来构建新方法的MethodDeclaration
    private MethodDeclaration extractNewMethod(CompilationUnit cu, MethodDeclaration method, String mNewSource, SourceRule sourceRule){
        method.delete();
        String firstType = cu.types().get(0).toString();
        StringBuilder remainTypeBuilder = new StringBuilder();
        for(int i=1;i<cu.types().size();i++){
            remainTypeBuilder.append(cu.types().get(i).toString());
        }
        for(Object type : new ArrayList<>(cu.types())){
            cu.types().remove(type);
        }
        StringBuilder cuSource = new StringBuilder();
        cuSource.append(cu.toString());
        cuSource.append(firstType.substring(0, firstType.lastIndexOf("}")) + mNewSource + "}\n");
        cuSource.append(remainTypeBuilder.toString());
        cu = parseAST(cuSource.toString(), sourceRule);
        return extractMethod(cu, sourceRule);
    }

    private CompilationUnit parseAST(String sourceCode, SourceRule sourceRule){
        File apiFile = new File(sourceRule.getFileLocation());
        String apiJarDir = sourceRule.getLibraryJarPath(libRootDir);
        return (CompilationUnit) JavaASTUtil.parseSource(sourceCode, apiFile.getAbsolutePath(), apiFile.getName(), apiJarDir== null? null:new String[]{apiJarDir});
    }

    public Map<String, SourceRule> readRuleCSV(Set<String> apis, String csvPath) throws IOException {
        Map<String, SourceRule> sourceRuleMap = new LinkedHashMap<>();
        Map<String, String> excToCondRuleIdMap = new HashMap<>();
        Map<String, List<String>> orderAlterRuleIdMap = new HashMap<>();
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader(new FileInputStream(new File(csvPath))));
        int lineNum = 0;
        for (CSVRecord record : records) {
            if (record==null || record.size()<=0){
                continue;
            }
            lineNum++;
            SourceRule sourceRule = new SourceRule();
            sourceRule.setId(record.get("Id"));
            sourceRule.setType(SourceRule.SourceRuleType.fromLabel(record.get("Type")));
            if(sourceRule.getType() == null){
                continue;
            }

            sourceRule.setApiClass(record.get("Class"));
            if(!CollectionUtils.isEmpty(apis) && !apis.contains(sourceRule.getApiClass())){//apis为null时全部api都输出
                continue;
            }

            sourceRule.setApiMethod(record.get("API Method"));
            if(sourceRule.getApiMethod().contains(",") && !sourceRule.getApiMethod().contains(", ")){
                sourceRule.setApiMethod(sourceRule.getApiMethod().replaceAll(",", ", "));
            }
            sourceRule.setLibName(record.get("Library Name"));
            sourceRule.setLibVersion(record.get("Library Version"));

            if(apiLocationMap.get(sourceRule.getApiClass())==null){
                addAPIFileLocation(sourceRule);
            }
            sourceRule.setFileLocation(apiLocationMap.get(sourceRule.getApiClass()));

            sourceRule.setStartLine(Integer.valueOf(record.get("Start Line")));
            sourceRule.setStartLine(Integer.valueOf(record.get("End Line")));
            switch (sourceRule.getType()){
                case Exception:
                    sourceRule.setException(record.get("Exception"));
                    String exceptionCondition = record.get("Exception Condition");
                    if(exceptionCondition!=null){
                        excToCondRuleIdMap.put(sourceRule.getId(), exceptionCondition);
                    }
                    break;
                case Order:
                    sourceRule.setSequence(new ArrayList<>(Arrays.asList(record.get("Pre Order"), "this")));
                    orderAlterRuleIdMap.put(sourceRule.getId(), Arrays.asList(record.get("Restrain Id").split(";")));
                    break;
                case Condition:
                    sourceRule.setCondition(record.get("Condition"));
                    break;
            }
            sourceRuleMap.put(sourceRule.getId(), sourceRule);
        }
        System.out.println("SourceRuleFile lineNum: " + lineNum);
        //设置exceptionRule和conditionRule映射关系
        for(String ruleId : excToCondRuleIdMap.keySet()) {
            SourceRule conRule = sourceRuleMap.get(excToCondRuleIdMap.get(ruleId));
            sourceRuleMap.get(ruleId).setExceptionCondition(conRule);
        }
        //设置orderRule之前的替换关系
        for(String ruleId: orderAlterRuleIdMap.keySet()){
            sourceRuleMap.get(ruleId).setAlterOrders(
                    orderAlterRuleIdMap.getOrDefault(ruleId, new ArrayList<>())
                            .stream().map(id->sourceRuleMap.getOrDefault(id, null))
                            .filter(rule-> rule != null).collect(Collectors.toList()));
        }

        return sourceRuleMap;
    }

    public Map<String, SourceRule> readRuleExcel(Set<String> apis, String excelPath) throws IOException {
        Map<String, SourceRule> sourceRuleMap = new LinkedHashMap<>();
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(new FileInputStream(new File(excelPath)));
        XSSFSheet sheet = xssfWorkbook.getSheetAt(0);//读取第一个工作表
        Map<String, String> excToCondRuleIdMap = new HashMap<>();
        for(int i=1;i <= sheet.getLastRowNum();i++){
            XSSFRow row = sheet.getRow(i);
            if(row == null){
                break;
            }
            SourceRule sourceRule = new SourceRule();
            sourceRule.setId(String.valueOf(i));
            sourceRule.setType(SourceRule.SourceRuleType.fromLabel(row.getCell(6).getStringCellValue()));
            if(sourceRule.getType() == null){
                continue;
            }

            sourceRule.setApiClass(row.getCell(1).getStringCellValue());
            if(CollectionUtils.isEmpty(apis) || !apis.contains(sourceRule.getApiClass())){
                continue;
            }

            sourceRule.setApiMethod(row.getCell(0).getStringCellValue());
            sourceRule.setLibName(row.getCell(2).getStringCellValue());
            sourceRule.setLibVersion(row.getCell(3).getStringCellValue());

            if(apiLocationMap.get(sourceRule.getApiClass())==null){
                addAPIFileLocation(sourceRule);
            }
            sourceRule.setFileLocation(apiLocationMap.get(sourceRule.getApiClass()));

            sourceRule.setStartLine(Double.valueOf(row.getCell(4).getNumericCellValue()).intValue());
            sourceRule.setEndLine(Double.valueOf(row.getCell(5).getNumericCellValue()).intValue());
            switch (sourceRule.getType()){
                case Exception:
                    sourceRule.setException(row.getCell(8).getStringCellValue());
                    if(row.getCell(9)!=null){
                        excToCondRuleIdMap.put(sourceRule.getId(), row.getCell(9).getStringCellValue());
                    }
                    break;
                case Order:
                    sourceRule.setSequence(new ArrayList<>(Arrays.asList(row.getCell(10).getStringCellValue().split(";"))));
                    break;
                case Condition:
                    sourceRule.setCondition(row.getCell(11).getStringCellValue());
                    break;
            }
            sourceRuleMap.put(sourceRule.getId(), sourceRule);
        }
        //设置exceptionRule和conditionRule映射关系
        for(String ruleId : excToCondRuleIdMap.keySet()) {
            SourceRule conRule = sourceRuleMap.get(excToCondRuleIdMap.get(ruleId));
            sourceRuleMap.get(ruleId).setExceptionCondition(conRule);
        }
        return sourceRuleMap;
    }

    private void addAPIFileLocation(SourceRule rule) throws IOException {
        String relativePath = rule.getApiClass().replace(".", "/") + ".java";
        String libDir = rule.getLibrarySrcPath(libRootDir);
        Files.walk(Paths.get(libDir))
                .filter(path -> path.toFile().getAbsolutePath().endsWith(relativePath))
                .forEach(path ->  apiLocationMap.put(rule.getApiClass(), path.toFile().getAbsolutePath()));
    }

    public static String buildMethodCall(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder();
        if(method.isConstructor()){
            sb.append("new ");
        }
        sb.append(method.getName().getIdentifier() + "(");
        for (int i = 0; i < method.parameters().size(); i++) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) method.parameters().get(i);
            sb.append(svd.getName().getIdentifier());
            if(i!=method.parameters().size()-1){
                sb.append(", ");
            }
        }
        sb.append(");");
        return sb.toString();
    }

    //将条件中的arg*参数形式转换成实际参数名
    public static String buildCondition(MethodDeclaration method, String condition) {
        if(!condition.contains("arg")){
            return condition;
        }
        if(method.parameters().size()==0){
            return null;
        }
        String conResult = condition;
        Pattern r = Pattern.compile("arg\\d+");
        Matcher m = r.matcher(condition);
        while (m.find()) {
            String arg = m.group();
            int argIndex = Integer.valueOf(arg.replace("arg", ""));
            String argName = ((SingleVariableDeclaration)method.parameters().get(argIndex)).getName().getIdentifier();
            conResult = conResult.replaceAll(arg, argName);
        }
        return conResult;
    }

    //提取条件语句中的参数
    public static Set<String> extractRevolveParams(MethodDeclaration method, String condition){
        Set<String> revolveParams = new LinkedHashSet<>();
        if(!condition.contains("arg")){
            return revolveParams;
        }
        Pattern r = Pattern.compile("arg\\d+");
        Matcher m = r.matcher(condition);
        while (m.find()) {
            revolveParams.add(m.group());
        }
        return revolveParams;
    }

    private static String paramNameToIndex(MethodDeclaration method, String paramName){
        for (int i = 0; i < method.parameters().size(); i++) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) method.parameters().get(i);
            if(svd.getName().getIdentifier().equals(paramName)){
                return "arg" + i;
            }
        }
        return null;
    }

    private boolean isConstructor(String methodSig){
        if(methodSig.lastIndexOf(".")<0){
            return false;
        }
        String className = methodSig.substring(0, methodSig.lastIndexOf("."));
        className = className.substring(className.lastIndexOf(".")+1);
        String methodName = AUGBuilder.toMethodName(methodSig.split("\\(")[0]);
        if(!StringUtils.isEmpty(methodName) && !StringUtils.isEmpty(className) && methodName.equals(className)){
            return true;
        }
        return false;
    }

    private String getTypeName(String methodSig){
        String className = methodSig.substring(0, methodSig.lastIndexOf("."));
        return className.substring(className.lastIndexOf(".")+1);
    }

    //没有类名前缀
    private String getPureMethodSig(String methodSig){
        return methodSig.substring(methodSig.lastIndexOf(".")+1);
    }

    public static APIUsageGraph removeAloneNodes(APIUsageGraph aug){
        for(Node node : new ArrayList<>(aug.vertexSet())){
            if(aug.incomingEdgesOf(node).size() == 0 && aug.outgoingEdgesOf(node).size() == 0){
                aug.removeVertex(node);
            }
        }
        return aug;
    }

}
