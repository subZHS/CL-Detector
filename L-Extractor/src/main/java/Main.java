import GainPatterns.AstAnalyse;
import RuleFormat.LibParam;
import RuleFormat.Rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Main {


    public static void main(String args[]) throws IOException {
        AstAnalyse astAnalyse=new AstAnalyse();

        File file = new File("lib/codeList.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                String[] strArr = tempString.split("\t");
                String file_path = findPath(strArr);
                LibParam libParam = new LibParam(strArr[0],strArr[1],strArr[2]);
//                if(strArr[2].equals("java.io.DataOutputStream")){
                    astAnalyse.anASt(file_path,libParam);

//                }

            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(astAnalyse.getRules().size());
        List<Rules> rulesList=astAnalyse.getRules();
        OutPut outPut =new OutPut();
        outPut.ToExcel(rulesList);
        System.out.println("okkkk");

    }


    /*public static void main(String args[]) {

    }*/

    private static String findPath(String[] className){
        String[] strArr = className[2].split("\\.");
        String pathName = "lib/libSrc/libSrc";
        String libName = className[0]+"_"+className[1];
        String fileName="/source";
        for(int i=0;i<strArr.length;i++){
            fileName+=("/"+strArr[i]);
        }
        fileName+=".java";
        return pathName+"/"+libName+fileName;
    }
}
