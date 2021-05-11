package RuleFormat;

public class LibParam {
    private String libName;
    private String libVersion;
    private String className;

    public LibParam(String libName, String libVersion, String className) {
        this.libName = libName;
        this.libVersion = libVersion;
        this.className = className;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
