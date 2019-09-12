package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import com.abc.util.DateTimeUtils;
import freemarker.template.TemplateModel;

import java.util.Date;
import java.util.List;

public class NowAddFunction extends CustomFunction {
    private static final String FUNCTION_NAME = "nowAdd";

    private static final String DEFAULT_FORMAT = "yyyyMMddHHmmssSSS";
    private static final String DESC = String.format("函数%s格式:${%s(%s,%s,%s)}", FUNCTION_NAME, FUNCTION_NAME, DEFAULT_FORMAT, 5, "hour");

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getFunctionDesc() {
        return DESC;
    }

    /**
     * ${nowAdd('timestamp', 2, 'day')} return 1546347600232  当前时间加2天
     *
     * @param paramList
     * @return
     */
    @Override
    public String execFunction(List paramList) {
        if (paramList.size() == 3) {
            String format = FreemarkerUtils.getObject(((TemplateModel) paramList.get(0))).toString();
            String amount = FreemarkerUtils.getObject(((TemplateModel) paramList.get(1))).toString();
            String unit = FreemarkerUtils.getObject(((TemplateModel) paramList.get(2))).toString();
            return DateTimeUtils.add(format, new Date(), Integer.parseInt(amount), unit);
        }
        throw new MessageRuntimeException("参数个数不正确");
    }

}