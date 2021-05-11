package RuleFormat;

public class Rules {
    private long id;
    private String method;
    private String libraryName;
    private String libraryVersion;
    private String apiClass;
    private int startLine;
    private int endLine;
    private String type;
    private String except;
    private String exceptCon;
    private String nextOrder;
    private String condition;
    private String restrainId;

    public Rules(long id,String method, String libraryName, String libraryVersion, String apiClass,
                 int startLine, int endLine, String type, String except, String exceptCon, String nextOrder, String condition, String restrainId) {
        this.id = id;
        this.method = method;
        this.libraryName = libraryName;
        this.libraryVersion = libraryVersion;
        this.apiClass = apiClass;
        this.startLine = startLine;
        this.endLine = endLine;
        this.type = type;
        this.except = except;
        this.exceptCon = exceptCon;
        this.nextOrder = nextOrder;
        this.condition = condition;
        this.restrainId = restrainId;
    }

    public Rules(String method,String except,String exceptCon){
        this.method=method;
        this.except = except;
        this.exceptCon = exceptCon;
    }
    //getter
    public long getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getLibraryVersion() {
        return libraryVersion;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getType() {
        return type;
    }

    public String getExcept() {
        return except;
    }

    public String getExceptCon() {
        return exceptCon;
    }

    public String getNextOrder() {
        return nextOrder;
    }

    public String getRestrainId() {
        return restrainId;
    }
    //setter
    public void setId(long id) {
        this.id = id;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public void setLibraryVersion(String libraryVersion) {
        this.libraryVersion = libraryVersion;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setExcept(String except) {
        this.except = except;
    }

    public void setExceptCon(String exceptCon) {
        this.exceptCon = exceptCon;
    }

    public void setNextOrder(String nextOrder) {
        this.nextOrder = nextOrder;
    }

    public void setRestrainId(String restrainId) {
        this.restrainId = restrainId;
    }

    public String getApiClass() {
        return apiClass;
    }

    public void setApiClass(String apiClass) {
        this.apiClass = apiClass;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
