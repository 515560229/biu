package com.abc.util.freemarker;

import com.abc.entity.VariableConfig;
import com.abc.exception.MessageRuntimeException;
import com.abc.service.VariableConfigService;
import com.abc.util.SpringContextUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import freemarker.template.TemplateModel;

import java.util.List;

public class GetVarFunction extends CustomFunction {
    private static final String FUNCTION_NAME = "getVar";

    private static final String DESC = String.format("函数%s格式:${%s('%s')}}", FUNCTION_NAME, FUNCTION_NAME, "变量名");

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getFunctionDesc() {
        return DESC;
    }

    /**
     * ${getVar("var")}
     *
     * @param paramList
     * @return
     */
    @Override
    public String execFunction(List paramList) {
        if (paramList.isEmpty()) {
            throw new MessageRuntimeException("参数不能为空");
        }
        String firstParameter = FreemarkerUtils.getObject((TemplateModel) paramList.get(0)).toString();
        VariableConfigService variableConfigService = SpringContextUtils.INSTANCE.getBean(VariableConfigService.class);

        EntityWrapper<VariableConfig> wrapper = new EntityWrapper<>();
        wrapper.eq("name", firstParameter);
        VariableConfig variableConfig = variableConfigService.selectOne(wrapper);
        if (variableConfig == null) {
            throw new MessageRuntimeException("变量名:" + firstParameter + "不存在");
        }
        return variableConfig.getValue();
    }
}
