package GainPatterns;

import RuleFormat.FieldConstruction;
import RuleFormat.LibParam;
import RuleFormat.Rules;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Joiner;

import java.io.*;
import java.util.*;

public class AstAnalyse {
    private List<Rules> allRule =new ArrayList<Rules>();
    private long idList=0;
    private static LibParam libParam;
    private List<FieldConstruction> fieldConstructions = new ArrayList<>();
    private List<FieldConstruction> conList = new ArrayList<>();
    private List<FieldConstruction> useList = new ArrayList<>();
    public void anASt(String FILE_PATH, LibParam libParam) throws FileNotFoundException {
        this.libParam = libParam;
       try {
           fieldConstructions.clear();
           conList.clear();
           useList.clear();
            CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));
            VoidVisitor<Void> methodNameVisitor = new MethodNamePrinter();
            methodNameVisitor.visit(cu, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
       orderRule();
    }

    public void clearRule(){
        allRule.clear();
    }
    public void addRule1(Rules rules){
        allRule.add(rules);
    }

    private class MethodNamePrinter extends VoidVisitorAdapter<Void> {
        private String ExceptCon = "";
        private String APIName = "";
        private Map<String, String> params = new LinkedHashMap<String, String>();
        private String paramLimit = "";

        @Override
        public void visit(FieldDeclaration fd, Void arg){
            super.visit(fd, arg);
            params.clear();
            NodeList modifiers = fd.getModifiers();
            if(modifiers.size()!=0){
                if(fd.getParentNode().isPresent()){
                    try{
                        ClassOrInterfaceDeclaration coi= (ClassOrInterfaceDeclaration) fd.getParentNode().get();
                        NodeList parentMo = coi.getModifiers();
                        int modiSizePa = parentMo.size();
                        String pubOrSon = coi.getModifiers().get(0).toString();
                        if(pubOrSon.equals("public")||pubOrSon.equals("public ")) {
                            //父类是public类
                            if(joinConstruct(modifiers)){
                                try{
                                    VariableDeclarator variableDeclarator = (VariableDeclarator) fd.getChildNodes().get(1);
                                    String classParamType = variableDeclarator.getType().toString().trim();
                                    String classParam = fd.getVariable(0).toString().trim();
                                    if(classParamType.equals("int")||classParamType.equals("boolean"))
                                        return;
                                    String [] arr = classParam.split("\\s+");
                                    classParam = arr[0];
                                    FieldConstruction fieldConstruction = new FieldConstruction
                                            (classParamType,classParam, 0);
                                    fieldConstructions.add(fieldConstruction);
                                }catch (Exception e){
                                    System.out.println("not variable");
                                }


                            }
                        }
                    }catch (Exception e){
                        System.out.println("coi");
                    }
                }
            }


        }

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            params.clear();
            int exceptSize = md.getThrownExceptions().size();
            Optional<BlockStmt> a = md.getBody();
            if (!a.isPresent())
                return;
            int stmtNum = a.get().getStatements().size();
            APIName = md.getNameAsString();
            NodeList param = md.getParameters();
            if (md.getModifiers().size() != 0) {
                if (!md.getModifiers().get(0).getKeyword().toString().equalsIgnoreCase("PUBLIC")) {
                    return;
                }
            }
            if (param.isEmpty()) {
                paramLimit = "()";
                Optional<Comment> javadocComment = md.getComment();
                if (javadocComment.isPresent()) {
                    String content = javadocComment.get().getContent();
                    Range rangeParent = md.getRange().get();
                    int startP = rangeParent.begin.line;
                    int endP = rangeParent.end.line;
                    String[] strArr = content.split("\n");
                    for (int i = 0; i < strArr.length; ++i){
                        if(strArr[i].contains("@exception")||strArr[i].contains("@throws")){
                            String exceptionDescribe = strArr[i];
                            exceptionDescribe = exceptionDescribe.trim();
                            String[] ExceType = exceptionDescribe.split("\\s+");
                            if(!exceptionDescribe.equals("if")){
                                idList+=1;
                                Rules conditionRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                                        startP, endP,"exception",ExceType[2],"","","","0");
                                addRule1(conditionRule);
                            }

                        }
                    }

                }
            } else {
                int paramNum = param.size();
                paramLimit = "(";
                Parameter pa = (Parameter) param.get(0);
                paramLimit += pa.getTypeAsString();
                params.clear();//清空参数map
                params.put(pa.getName().toString(), pa.getTypeAsString());
                if (paramNum >= 1) {
                    for (int i = 1; i < paramNum; i++) {
                        pa = (Parameter) param.get(i);
                        String paName = pa.getName().toString();
                        String paType = pa.getTypeAsString();
                        params.put(paName, paType);
                        paramLimit += ("," + paType);
                    }
                }
                paramLimit += ")";
                paramLimit = paramLimit.replace("java.io.ObjectInputStream", "ObjectInputStream");
                Optional<Comment> javadocComment = md.getComment();
                if (javadocComment.isPresent()) {
                    String content = javadocComment.get().getContent();
                    Range rangeParent = md.getRange().get();
                    int startP = rangeParent.begin.line;
                    int endP = rangeParent.end.line;
                    findParamRule(content, startP, endP);
                }
            }
            for (int i = 0; i < stmtNum; i++) {
                Statement statement = md.getBody().get().getStatement(i);
                if (statement.isBlockStmt() || statement.isIfStmt() || statement.isThrowStmt())
                    findThrownStmt(statement);
            }

            //增加调用顺序
            for(FieldConstruction field1:fieldConstructions){
                if(field1.getCondition()==0){
                    String fieldName= field1.getName();
                    String mother = md.getBody().toString();
                    if(mother.contains(fieldName)){
                        String useString1 = fieldName+ " =";
                        String useString2 = fieldName+ "=";
                        if(mother.contains(fieldName+" =="))
                            continue;
                        else if(mother.contains(useString1)||mother.contains(useString2)){
                            FieldConstruction fieldUse = new FieldConstruction(APIName+paramLimit,field1.getName(),2);
                            conList.add(fieldUse);
                        }
                        else{
                            FieldConstruction fieldUse = new FieldConstruction(APIName+paramLimit,field1.getName(),3);
                            useList.add(fieldUse);
                        }
                    }
                }

            }
            //
            int k=0;
            for (Map.Entry<String, String> param1 : params.entrySet()) {
                String formalParam="arg"+k;
                String mapKey1 = param1.getKey()+"[0";
                String mapKey2 = param1.getKey()+"[0";
                String body = md.getBody().get().toString();
                if (body.contains(mapKey1) || body.contains(mapKey2)) {
                    System.out.println(md.toString());
                    idList += 1;
                    Rules ruleSize = new Rules(idList, this.APIName + this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                            md.getRange().get().begin.line, md.getRange().get().end.line, "condition", "", "", "", formalParam + ".size()>1", "0");
                    addRule1(ruleSize);

                }
                k += 1;
            }
        }

        @Override
        public void visit(ConstructorDeclaration md,Void arg){
            super.visit(md, arg);
            params.clear();
            BlockStmt a= md.getBody();
            int stmtNum= a.getStatements().size();
            APIName = md.getNameAsString();
            NodeList param = md.getParameters();
            if(md.getModifiers().size()!=0){
                if(md.getModifiers().get(0).getKeyword().toString()!="PUBLIC"){
                    return;
                }
            }
            if (param.isEmpty()) {
                paramLimit = "()";
            } else {
                int paramNum = param.size();
                paramLimit = "(";
                Parameter pa = (Parameter) param.get(0);
                paramLimit += pa.getTypeAsString();
                params.clear();//清空参数map
                params.put(pa.getName().toString(),pa.getTypeAsString());
                if(paramNum>=1){
                    for (int i = 1; i < paramNum; i++) {
                        pa = (Parameter) param.get(i);
                        String paName = pa.getName().toString();
                        String paType = pa.getTypeAsString();
                        params.put(paName,paType);
                        paramLimit += ("," + paType);

                    }
                }
                paramLimit += ")";
                paramLimit=paramLimit.replace("java.io.ObjectInputStream","ObjectInputStream");
                Optional<Comment> javadocComment = md.getComment();
                if(javadocComment.isPresent()){
                    String content = javadocComment.get().getContent();
                    Range rangeParent = md.getRange().get();
                    int startP = rangeParent.begin.line;
                    int endP = rangeParent.end.line;
                    findParamRule(content,startP,endP);
                }
            }
            for (int i = 0; i < stmtNum; i++) {
                Statement statement = md.getBody().getStatement(i);
                if (statement.isBlockStmt() || statement.isIfStmt() || statement.isThrowStmt())
                    findThrownStmt(statement);
            }
            makeConstruct(md.getBody());
        }

        private boolean joinConstruct(NodeList nodeList){
            int size = nodeList.size();
            if (size >= 3) {
                String node1 = nodeList.get(1).toString();
                if (node1.equals("static "))
                    return false;
                else{
                    System.out.println(nodeList.toString());
                    return true;
                }
            }
            else{
                System.out.println(nodeList.toString());
                return true;
            }
        }

        private void makeConstruct(BlockStmt blockStmt){
            for(FieldConstruction field1:fieldConstructions){
                    String fieldName= field1.getName();
                    fieldName=fieldName+" = ";
                    if(blockStmt.toString().contains(fieldName)){
//                        FieldConstruction fieldInit = new FieldConstruction(APIName+paramLimit,field1.getName(),1);
//                        conList.add(fieldInit);
                        field1.setCondition(1);
                    }
            }
        }

        private void findConstruct(BlockStmt blockStmt){
            for(FieldConstruction field1:fieldConstructions){
                int condition = field1.getCondition();
                if(condition == 0){
                    String fieldName= field1.getName();
                    if(blockStmt.toString().contains(fieldName)){
                        System.out.println(blockStmt);
                    }
                }
            }
        }

        private void findExcept(BlockStmt bt){
            ExceptCon = "";
            if(null == bt)
                return;
            if(null!=bt.getChildNodes()){
                int num = bt.getChildNodes().size();
                for(int i=0;i<num;i++){
                    Statement statement = bt.getStatements().get(i);
                    if(statement.isIfStmt()){
                        BlockStmt blockStmt = (BlockStmt) statement.asIfStmt().getThenStmt();
                        ExceptCon += statement.asIfStmt().getCondition().toString()+";";
                        if(null!=blockStmt){
                            String except = String.valueOf(statement.asThrowStmt().getExpression()
                                    .asObjectCreationExpr().getType().getName());
                            Range start = blockStmt.getRange().get();
                            int a = start.begin.line;
                            int b = start.end.line;
                            String condition = blockStmt.asIfStmt().getCondition().getChildNodes().get(0).toString();
                        }

                    }
                    else {
                        if(statement.isIfStmt()){
                            BlockStmt blockStmt =statement.asIfStmt().getThenStmt().asBlockStmt();
                            findExcept(blockStmt);
                            blockStmt =statement.asIfStmt().getElseStmt().get().asBlockStmt();
                            findExcept(blockStmt);
                        }
                        findExcept((BlockStmt) statement);
                    }
                }
            }
        }

        private void findThrownStmt(Statement blockStmt){
            if(!(blockStmt.isBlockStmt()|| blockStmt.isIfStmt()||blockStmt.isThrowStmt()))
                return;
            if(blockStmt.isThrowStmt())
                exceptRule(blockStmt);
            else{
                if(blockStmt.isIfStmt()){
                    Statement then = blockStmt.asIfStmt().getThenStmt();
                    if(then.isThrowStmt()){
                        exceptRule(then);
                        return;
                    }
                    if (then.isBlockStmt()) {
                        Optional<BlockStmt> thenS = then.toBlockStmt();
                        if(thenS.isPresent()){
                            BlockStmt thenStmt = thenS.get().asBlockStmt();
                            int thenSize = thenStmt.getStatements().size();
                            for (int i = 0; i < thenSize; i++) {
                                if(blockStmt.isBlockStmt()|| blockStmt.isIfStmt()||blockStmt.isThrowStmt())
                                    findThrownStmt(thenStmt.getStatement(i));
                            }
                        }
                    }
                    Optional<Statement> elseSt = blockStmt.asIfStmt().getElseStmt();
                    if (elseSt.isPresent()) {
                        Statement elseS = elseSt.get();
                        if(elseS.isThrowStmt()){
                            exceptRule(elseS);
                            return;
                        }
                        if(elseS.isBlockStmt()){
                            BlockStmt elseStmt = elseS.asBlockStmt();
                            int elseSize = elseStmt.getStatements().size();
                            for (int i = 0; i < elseSize; i++) {
                                if(blockStmt.isBlockStmt()|| blockStmt.isIfStmt()||blockStmt.isThrowStmt())
                                    findThrownStmt(elseStmt.getStatement(i));
                            }
                        }

                    }
                } else if (blockStmt.getChildNodes() != null) {
                    int childNum = blockStmt.getChildNodes().size();
                    for (int i = 0; i < childNum; i++) {
                        findThrownStmt(blockStmt.asBlockStmt().getStatement(i));
                    }
                }
            }
        }

        private void exceptRule(Statement blockStmt){
            Expression objectCreationExpr = blockStmt.asThrowStmt().getExpression();
            String exceptName ="";
            if(objectCreationExpr.isObjectCreationExpr())//如果是普通的描述
                exceptName = objectCreationExpr.asObjectCreationExpr().getTypeAsString();//异常名称
            if(objectCreationExpr.isMethodCallExpr())
                exceptName = objectCreationExpr.asMethodCallExpr().getNameAsString();
            String exceptCon = "";
            Range range = objectCreationExpr.getRange().get();
            int start = range.begin.line;
            int end = range.end.line;
            try{
                Statement parent = (Statement) blockStmt.getParentNode().get();
                while(!parent.isIfStmt()) {
                    parent = (Statement) parent.getParentNode().get();
                }
                int startP=0 ;
                int endP =0 ;
                while(parent.isIfStmt()){
                    exceptCon+=(parent.asIfStmt().getCondition().toString());
                    Range rangeParent = parent.getRange().get();
                    startP = rangeParent.begin.line;
                    endP = rangeParent.end.line;
                    parent = (Statement) parent.getParentNode().get();
                }
                if("getChars(int,int,char[],int)".equals(this.APIName+this.paramLimit)){
                    System.out.println("sss");
                }

                String argCon = changeCondition(exceptCon,params);
                if(!argCon.equals(exceptCon)){
                    if(!usefulRule(argCon))
                        return;
                    argCon=reverCondition(argCon);
                    idList+=1;
                    Rules conditionRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                            startP, endP,"condition","","","",argCon,"0");
                    addRule1(conditionRule);
                    idList+=1;
                    Rules rule1 = new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                            start, end, "exception", exceptName, String.valueOf(idList-1), "","", "0");
                    addRule1(rule1);
                }
            } catch (Exception e) {
                System.out.println("error");
            }

        }

        private String changeCondition(String condition, Map<String, String> paramCondition) {
            int i=0;
            String ret=condition;
            for (Map.Entry<String, String> param1 : paramCondition.entrySet()) {
                String formalParam="arg"+i;
                String mapKey = param1.getKey();
                if(mapKey.length()>3){
                    ret = ret.replace(mapKey,formalParam);
                }
                else{
                    ret = ret.replace(mapKey+" ",formalParam+" ");
                    ret = ret.replace(mapKey+".",formalParam+".");
                }
                i+=1;
            }
            return ret;
        }

        private boolean usefulRule(String condUse){
            String conUse = condUse;
            conUse = conUse.replaceAll(" ","");
            conUse = conUse.replaceAll("\\.","");
            conUse = conUse.replaceAll("arg","");
            conUse = conUse.replaceAll("\\d+","");
            conUse = conUse.replaceAll("<","");
            conUse = conUse.replaceAll(">","");
            conUse = conUse.replaceAll("=","");
            conUse = conUse.replaceAll("!","");
            conUse = conUse.replaceAll("\\(","");
            conUse = conUse.replaceAll("\\)","");
            conUse = conUse.replaceAll("size","");
            conUse = conUse.replaceAll("length","");
            conUse = conUse.replaceAll("\\|","");
            conUse = conUse.replaceAll("null","");
            conUse = conUse.replaceAll("L","");
            if(!conUse.equals("")){
//                System.out.println(conUse);
                return false;
            }
            return true;
        }

        private String reverCondition(String condition){
            String ret=condition;
            if(condition.contains("||")||condition.contains("&&"))
                return "!("+condition+")";
            if(condition.contains(">>")||condition.contains("<<"))
                return "!("+condition+")";
            if(ret.contains("==")){
                if(ret.contains("!=")){
                    ret=ret.replace("==","takePlace");
                    ret=ret.replace("!=","==");
                    ret=ret.replace("takePlace","!=");
                }
                else{
                    ret=ret.replace("==","!=");
                }
            }
            else{
                ret=ret.replace("!=","==");
            }
            if(ret.contains(">")){
                if(ret.contains("<")){
                    if(ret.contains(">=")){
                        ret=ret.replace(">=","waitPlace");
                        ret=ret.replace("<=",">");
                        ret=ret.replace("<",">=");
                        ret=ret.replace("waitPlace","<");
                    }
                    else{
                        ret=ret.replace(">","waitPlace");
                        ret=ret.replace("<=",">");
                        ret=ret.replace("<",">=");
                        ret=ret.replace("waitPlace","<=");
                    }
                }
                else{
                    ret=ret.replace(">=","<");
                    ret=ret.replace(">","<=");
                }
            }
            else{
                ret=ret.replace("<=",">");
                ret=ret.replace("<",">=");
            }
            return ret;
        }

        private void findParamRule(String comment,int startP,int endP){
            String[] strArr = comment.split("\n");
            for (int i = 0; i < strArr.length; ++i){
                if(strArr[i].contains("@throws")||strArr[i].contains("@exception")) {
                    String temp=strArr[i]+strArr[i+1];
                    String temp2 = temp.trim();
                    String[] ExceType = temp2.split("\\s+");
                    if(temp.contains("null")||temp.contains("NULL")||temp.contains("Null")){
                        String condition="";
                        int paramSize = this.params.size();
                        if(paramSize==1){
                            condition="arg0!=null";
                            idList+=1;
                            Rules conditionRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                                    startP, endP,"condition","","","",condition,"0");
                            addRule1(conditionRule);
                            idList+=1;
                            Rules exceptRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                                    startP, endP,"exception",ExceType[2],String.valueOf(idList-1),"","","0");
                            addRule1(exceptRule);
                        }
                        else{
                            if("setProperty(String,String)".equals(this.APIName+this.paramLimit)){
                                System.out.println("stop");
                            }
                            int mapSize=0;
                            int cursor=0;
                            for(String value : params.keySet()){
                                if(temp.contains(value)){
                                    condition="arg"+mapSize+"!=null";
                                    idList+=1;
                                    Rules conditionRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                                            startP, endP,"condition","","","",condition,"0");
                                    addRule1(conditionRule);
                                    idList+=1;
                                    Rules exceptRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                                            startP, endP,"exception",ExceType[2],String.valueOf(idList-1),"","","0");
                                    addRule1(exceptRule);
                                    cursor+=1;
                                }
                                mapSize+=1;
                            }
                            if(cursor==0){
                                idList+=1;
                                Rules exceptRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                                        startP, endP,"exception",ExceType[2],"","","","0");
                                addRule1(exceptRule);
                            }
                        }

                    }
                    else{
                        idList+=1;
                        Rules conditionRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
                                startP, endP,"exception",ExceType[2],"","","","0");
                        addRule1(conditionRule);
                    }
                }
//                if(strArr[i].contains("@param")){
//                    idList+=1;
//                    String cThis = changeCondition(strArr[i],params);
//                    Rules conditionRule=new Rules(idList,this.APIName+this.paramLimit, libParam.getLibName(), libParam.getLibVersion(), libParam.getClassName(),
//                            startP, endP,"param","","","",cThis,0);
//                    addRule1(conditionRule);
//                }
            }
        }
    }

    private boolean isLeave(Node father){
        if(null!=father.getChildNodes()){
            return true;
        }
        return false;
    }

    public List<Rules> getRules(){
        return allRule;
    }

    public void orderRule(){
        for(FieldConstruction filed1:fieldConstructions){
            if(filed1.getCondition()==0){
                List<FieldConstruction> conThisUse = new ArrayList<>();
                String fiName = filed1.getName();
                int conSize = 0;
                int useSize=0;
                for(FieldConstruction confield : conList){
                    if(confield.getName().equals(fiName)){
                        conSize+=1;
                        conThisUse.add(confield);
                    }
                }
                for(FieldConstruction useField:useList){
                    if(useField.getName().equals(fiName)){
                        useSize+=1;
                    }
                }
                if(conSize>0&&useSize>0){

                    for(FieldConstruction useField:useList){
                        if(useField.getName().equals(fiName)){
                            StringBuilder str= new StringBuilder();
                            for(long i=1;i<=conSize;i++){
                                long tempid=i+idList;
                                str.append(tempid);
                                str.append(";");
                            }
                            str = new StringBuilder(str.substring(0, str.length() - 1));
                            for(FieldConstruction conField:conThisUse){
                                idList+=1;
                                Rules ruleOrder = new Rules(idList,useField.getFieldType(),libParam.getLibName(),
                                        libParam.getLibVersion(), libParam.getClassName(),
                                        0,0,"order","","",conField.getFieldType(),"", str.toString());
                                addRule1(ruleOrder);
                            }

                        }
                    }
                }
            }

        }
    }
}
