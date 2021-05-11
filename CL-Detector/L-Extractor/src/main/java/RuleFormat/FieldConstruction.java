package RuleFormat;

public class FieldConstruction {
    private String name;
    private String fieldType;
    private int condition;

    public FieldConstruction (String fieldType,String name, int condition){
        this.name=name;
        this.fieldType = fieldType;
        this.condition=condition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public int getCondition() {
        return condition;
    }

    public void setCondition(int condition) {
        this.condition = condition;
    }
}
