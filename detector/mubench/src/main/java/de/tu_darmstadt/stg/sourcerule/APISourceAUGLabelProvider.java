package de.tu_darmstadt.stg.sourcerule;

import de.tu_darmstadt.stg.mudetect.aug.model.data.VariableNode;
import de.tu_darmstadt.stg.mudetect.aug.visitors.BaseAUGLabelProvider;
import org.apache.commons.lang3.StringUtils;

public class APISourceAUGLabelProvider extends BaseAUGLabelProvider {

    @Override
    public String visit(VariableNode node) {
        return node.getType() + (StringUtils.isEmpty(node.getParamIndex())? "": String.format(" {%s}", node.getParamIndex()));
    }

}
