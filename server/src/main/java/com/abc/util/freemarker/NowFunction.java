package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NowFunction extends CustomFunction {
    private static final String FUNCTION_NAME = "now";

    private static final String DEFAULT_FORMAT = "yyyyMMddHHmmssSSS";
    private static final String DESC = String.format("函数%s格式:${%s(%s)};${%s(%s)}", FUNCTION_NAME, FUNCTION_NAME, "", FUNCTION_NAME, DEFAULT_FORMAT);

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
        if (paramList.size() > 1) {
            throw new MessageRuntimeException("参数个数不能>=2");
        }
        if (paramList.size() == 1) {
            return new SimpleDateFormat(FreemarkerUtils.getObject(((TemplateModel) paramList.get(0))).toString())
                    .format(new Date());
        }
        return new SimpleDateFormat(DEFAULT_FORMAT).format(new Date());
    }
}