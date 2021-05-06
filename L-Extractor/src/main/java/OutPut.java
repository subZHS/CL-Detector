import RuleFormat.Rules;
import com.csvreader.CsvWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class OutPut {
    public void ToExcel(List<Rules> rulesList) throws IOException {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");//设置日期格式
        String file_name = "rule_"+df.format(new Date());
        String filePath = file_name+".csv";//文件命名
        try {
            // 创建CSV写对象
            CsvWriter csvWriter = new CsvWriter(filePath,',', Charset.forName("GBK"));
            //CsvWriter csvWriter = new CsvWriter(filePath);

            // 写表头
            String[] headers = {"Id", "API Method", "Library Name", "Library Version", "Class",
                    "Start Line", "End Line", "Type", "Exception", "Exception Condition", "Pre Order", "Condition","Restrain Id"};
            csvWriter.writeRecord(headers);

            //写内容
            int length=rulesList.size();
            for(int i=0;i<length;i++){
                Rules temp = rulesList.get(i);
                String[] content = RuleToString(temp);
                csvWriter.writeRecord(content);
            }
            csvWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //类到数组的转换
    public String[] RuleToString(Rules temp){
        String[] oneRule = new String[13];
        oneRule[0] = String.valueOf(temp.getId());
        oneRule[1] = temp.getMethod();
        oneRule[2] = temp.getLibraryName();
        oneRule[3] = temp.getLibraryVersion();
        oneRule[4] = temp.getApiClass();
        oneRule[5] = String.valueOf(temp.getStartLine());
        oneRule[6] = String.valueOf(temp.getEndLine());
        oneRule[7] = temp.getType();
        oneRule[8] = temp.getExcept();
        oneRule[9] = temp.getExceptCon();
        oneRule[10] = String.valueOf(temp.getNextOrder());
        oneRule[11] = temp.getCondition();
        oneRule[12] = String.valueOf(temp.getRestrainId());

        return oneRule;
    }

}
