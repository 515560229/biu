package com.abc.util.freemarker;


import com.abc.exception.MessageRuntimeException;
import freemarker.template.TemplateMethodModelEx;

import java.util.List;
import java.util.UUID;

public class UUIDFunction extends CustomFunction {
    private static final String FUNCTION_NAME = "uuid";
    private static final String DESC = String.format("函数%s格式:${%s(%s)}", FUNCTION_NAME, FUNCTION_NAME, "");

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getFunctionDesc() {
        return DESC;
    }

    @Override
    public String execFunction(List paramList) {
        if (!paramList.isEmpty()) {
            throw new MessageRuntimeException("参数个数不正确");
        }
        return UUID.randomUUID().toString();
    }
}