package de.tu_darmstadt.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.MuBenchRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MuDetectRunner {

    public static String precisionResultDirName = "precisionResult";
    public static String resultDirName = "results";

    public static void main(String[] args) throws Exception {
        //TODO (myCode)
        args = supplyArgs(args);
        new MuBenchRunner()
                .withDetectOnlyStrategy(new ProvidedPatternsStrategy())
                .withMineAndDetectStrategy(new IntraProjectStrategy())
                .run(args);
    }

    public static String[] supplyArgs(String[] args){
        //TODO (myCode)
        List<String> argList = new ArrayList<>(Arrays.asList(args));

        boolean isPrecisionExp = false;
        for(int i = 0; i < args.length; i += 2) {
            if(args[i].equals("is_precision_exp")){
                isPrecisionExp = Boolean.valueOf(args[i+1]);
                argList.remove(args[i]);
                argList.remove(args[i+1]);
                break;
            }
        }

        for(int i = 0; i < args.length; i += 2) {
            String arg = args[i];
            if (arg.equals("target_src_path")){
                File resultDir;
                if(isPrecisionExp){
                    resultDir = getResultDir(precisionResultDirName, args[i+1], true);
                }else {
                    resultDir = getResultDir(args[i + 1], true);
                }
                argList.add("target");
                argList.add(resultDir.getAbsolutePath() + "/findings-output.yml");
                argList.add("run_info");
                argList.add(resultDir.getAbsolutePath() + "/run-info-output.yml");
                break;
            }
        }

        args = argList.toArray(new String[0]);
        return args;
    }

    //TODO (myCode)
    public static File getResultDir(String targetSrcPath, boolean create){
        return getResultDir(resultDirName, targetSrcPath, create);
    }

    public static File getResultDir(String resultDirName, String targetSrcPath, boolean create){
        List<String> splits = Arrays.asList(targetSrcPath.split(File.separator));
        if(splits.get(splits.size()-1).isEmpty()){
            splits.remove(splits.size()-1);
        }
        String projectName = splits.get(splits.size()-2) + File.separator + splits.get(splits.size()-1);

        File resultDir = new File(resultDirName + File.separator + projectName);
        if (!resultDir.exists() && create) {
            resultDir.mkdirs();
        }
        return resultDir;
    }
}
