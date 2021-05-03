package de.tu_darmstadt.stg.mubench;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class GitUtil {

    public static void checkout(String projectPath, String commitID) throws Exception{
        Repository repo = new FileRepository(projectPath + File.separator + ".git");
        Git git = new Git(repo);
        git.checkout().setName(commitID).setForced(true).call();
        repo.close();
    }

    /**
     * 找到某个commit的上一个commit,紧跟着当前commit的10bit的完整commitId号
     */
    public static List<String> findPreCommit(String projectPath, String commitId){
        String[] command = {"bash","-c", "cd "+ projectPath +";git log -2 --pretty='commit: %H - %s' " + commitId};
        try {
            String result = runWithoutPrint(command);
            String[] lines =result.split("\\\n");
            List<String> twoCommitId = new ArrayList<>();
            for(String line : lines){
                if(line.startsWith("commit: ")){
                    String tmp = line.replace("commit: ","");
                    if(commitId.length()<=10){
                        twoCommitId.add(tmp.split(" - ")[0].substring(0,10));
                    }else{
                        twoCommitId.add(tmp.split(" - ")[0]);
                    }
                }
                if(twoCommitId.size()>=2){
                    break;
                }
            }
            Collections.reverse(twoCommitId);
            return twoCommitId;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String runWithoutPrint(String[] command) throws IOException {
        Scanner input = null;
        String result = "";
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            try {
                //等待命令执行完成
                process.waitFor(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            InputStream is = process.getInputStream();
            input = new Scanner(is);
            while (input.hasNextLine()) {
                result += input.nextLine() + "\n";
            }
            input.close();
            input = new Scanner(process.getErrorStream());
            while (input.hasNextLine()) {
                result += input.nextLine() + "\n";
            }
        } finally {
            if (input != null) {
                input.close();
            }
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }
}
