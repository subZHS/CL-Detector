//package GainPatterns;
//
//import org.eclipse.jdt.core.dom.*;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class CodeParser {
//    public static int count;
//    //使用node type作为数组中的下标
//    public static List<Map<Object, Integer>> countList = new ArrayList<>(101);
//
//    //use ASTParse to parse string
//    public static void parse(String str) {
//        initialize();
//        ASTParser parser = ASTParser.newParser(8);
//        parser.setSource(str.toCharArray());
//        parser.setKind(ASTParser.K_COMPILATION_UNIT);
//
//        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//
//
//        cu.accept(new ASTVisitor() {
//            public void preVisit(ASTNode node) {
//                List list = node.structuralPropertiesForType();
//                node.setProperty("isTerminal", 1);
//                for (Object o : list) {
//                    if (o.getClass().getName().contains("Child")) {
//                        node.setProperty("isTerminal", 0);
//                        break;
//                    }
//                }
//            }
//
//        });
//        cu.accept(new ASTVisitor() {
//
//            public void preVisit(ASTNode node) {
//                int index = node.getNodeType();
//                System.out.println(node + "\n:");
//                Map<Object, Integer> map = countList.get(index);
//                List list = node.structuralPropertiesForType();
//                for (Object o : list) {
//                    if (o instanceof ChildListPropertyDescriptor) {
//                        ChildListPropertyDescriptor d = (ChildListPropertyDescriptor) o;
//                        List<ASTNode> childList = (List<ASTNode>) node.getStructuralProperty(d);
//                        for (ASTNode child : childList) {
//
//                            Object key;
//                            if (child.getProperty("isTerminal").equals(0)) {
//                                key = child.getNodeType();
//                            } else {
//                                key = child.toString();
//                            }
//                            if (map.containsKey(key)) {
//                                map.put(key, map.get(key) + 1);
//                            } else {
//                                map.put(key, 1);
//                            }
//                        }
//
//                    } else if (o instanceof ChildPropertyDescriptor) {
//                        ChildPropertyDescriptor d = (ChildPropertyDescriptor) o;
//                        ASTNode child = (ASTNode) node.getStructuralProperty(d);
//
//                        Object key;
//                        if (child != null) {
//                            if (child.getProperty("isTerminal").equals(0)) {
//                                key = child.getNodeType();
//                            } else {
//                                key = child.toString();
//                            }
//                            if (map.containsKey(key)) {
//                                map.put(key, map.get(key) + 1);
//                            } else {
//                                map.put(key, 1);
//                            }
//                        }
//
//                    }
//                }
//                System.out.println("------------------");
//
//            }
//        });
//    }
//
//    //read file content into a string
//    public static String readFileToString(String filePath) throws IOException {
//        StringBuilder fileData = new StringBuilder();
//        BufferedReader reader = new BufferedReader(new FileReader(filePath));
//
//        char[] buf = new char[10];
//        int numRead = 0;
//        while ((numRead = reader.read(buf)) != -1) {
//            String readData = String.valueOf(buf, 0, numRead);
//            if (readData.startsWith("package") || readData.startsWith("import")) {
//                continue;
//            }
//            fileData.append(readData);
//            buf = new char[1024];
//        }
//
//        reader.close();
//        return fileData.toString();
//    }
//
//    //loop directory to get file list
//    public static void parseFilesInDir(String dirPath) throws IOException {
//        File root = new File(dirPath);
//        File[] files = root.listFiles();
//        String filePath = null;
//
//        for (File f : files) {
//            filePath = f.getAbsolutePath();
//
//            if (f.isFile()) {
//                if (filePath.endsWith(".java")) {
//
//                    parse(readFileToString(filePath));
//                }
//
//            } else {
//                //去掉测试文件
//                if (!filePath.contains("/test/")) {
//                    parseFilesInDir(filePath);
//                }
//
//            }
//        }
//    }
//
//    private static void initialize() {
//        for (int i = 0; i < 101; i++) {
//            Map<Object, Integer> map = new HashMap<>();
//            countList.add(map);
//        }
//    }
//
//}
