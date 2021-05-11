package de.tu_darmstadt.stg.mubench;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.nio.file.Paths;

import static de.tu_darmstadt.stg.sourcerule.SourceRuleParser.libRootDir;

public class API {
    private final String qualifiedName;
    //TODO (myCode)
    private String libName;
    private String libVersion;

    public API(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getName() {
        return qualifiedName;
    }

    public String getSimpleName() {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    @Override
    public String toString() {
        return qualifiedName;
    }

    //TODO (myCode)
    public API fulfillLibAttr() throws Exception {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(new FileInputStream(Paths.get(libRootDir, "dataset.xlsx").toFile()));
        XSSFSheet sheet = xssfWorkbook.getSheetAt(3);//读取第二个工作表
        for(int i=1;i <= sheet.getLastRowNum();i++){
            XSSFRow row = sheet.getRow(i);
            if(row == null){
                break;
            }
            row.getCell(0).setCellType(CellType.STRING);
            row.getCell(1).setCellType(CellType.STRING);
            row.getCell(2).setCellType(CellType.STRING);
            String apiCell = row.getCell(2).getStringCellValue();
            if(apiCell.equals(getName())){
                setLibName(row.getCell(0).getStringCellValue());
                setLibVersion(row.getCell(1).getStringCellValue());
                if(StringUtils.isEmpty(getLibName()) || StringUtils.isEmpty(getLibVersion())){
                    return null;
                }
                return this;
            }
        }
        return null;
    }

    public String getLibDirName() throws Exception {
        if(getLibName() == null){
            if(fulfillLibAttr() == null){
                return null;
            }
        }
        return getLibDirName(getLibName(), getLibVersion());
    }

    private static String getLibDirName(String libName, String libVersion){
        return libName + "_" + libVersion;
    }

    public String getLibName() {
        return libName;
    }

    public void setLibName(String libName) {
        this.libName = libName;
    }

    public String getLibVersion() {
        return libVersion;
    }

    public void setLibVersion(String libVersion) {
        this.libVersion = libVersion;
    }
}
