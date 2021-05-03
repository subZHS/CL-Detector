package de.tu_darmstadt.stg.mudetect.aug.model.data;

import de.tu_darmstadt.stg.mudetect.aug.model.BaseNode;
import de.tu_darmstadt.stg.mudetect.aug.model.DataNode;
import de.tu_darmstadt.stg.mudetect.aug.visitors.NodeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VariableNode extends BaseNode implements DataNode {
    private final String variableType;
    private final String variableName;
    private String paramIndex;

    public VariableNode(String variableType, String variableName) {
        this.variableType = variableType;
        this.variableName = variableName;
    }

    //TODO (myCode)添加参数序号
    public String getParamIndex() {
        if(paramIndex==null){
            return "";
        }
        return paramIndex;
    }

    public void setParamIndex(String paramIndex) {
        if(this.paramIndex != null){
            List<String> paramIndexItems = new ArrayList<>(Arrays.asList(this.paramIndex.split(", ")));
            paramIndexItems.add(paramIndex);
            this.paramIndex = String.join(", ", paramIndexItems);
        }else {
            this.paramIndex = paramIndex;
        }
    }

    @Override
    public String getName() {
        return variableName;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getType() {
        return variableType;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
