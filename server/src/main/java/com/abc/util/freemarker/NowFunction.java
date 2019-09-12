package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import com.abc.util.DateTimeUtils;
import freemarker.template.TemplateModel;

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

    /**
     * ${now()} return 20190101210000232
     * ${now('yyyy-MM-dd HH:mm:ss')} return 2019-01-01 21:00:00
     * ${now('timestamp')} return 1546347600232
     *
     * @param paramList
     * @return
     */
    @Override
    public String execFunction(List paramList) {
        if (paramList.isEmpty()) {
            return DateTimeUtils.formatToString(new Date(), DEFAULT_FORMAT);
        }
        if (paramList.size() == 1) {
            return DateTimeUtils.formatToString(new Date(), FreemarkerUtils.getObject(((TemplateModel) paramList.get(0))).toString());
        }
        throw new MessageRuntimeException("参数个数不正确");
    }

}