package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import com.abc.util.DateTimeUtils;
import freemarker.template.TemplateModel;

import java.text.ParseException;
import java.util.List;

public class DateTimeAddFunction extends CustomFunction {
    private static final String FUNCTION_NAME = "datetimeAdd";

    private static final String DESC = String.format("函数%s格式:${%s(%s,%s,%s,%s)}", FUNCTION_NAME, FUNCTION_NAME,
            "yyyyMMddHHmmss", "20190805120000", 3, "hour");

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getFunctionDesc() {
        return DESC;
    }

    /**
     * ${datetimeAdd('yyyyMMddHHmmss', '20190805120000', 2, 'day')}
     *
     * @param paramList
     * @return
     */
    @Override
    public String execFunction(List paramList) {
        if (paramList.size() == 4) {
            String pattern = FreemarkerUtils.getObject(((TemplateModel) paramList.get(0))).toString();
            String dateStr = FreemarkerUtils.getObject(((TemplateModel) paramList.get(1))).toString();
            String amount = FreemarkerUtils.getObject(((TemplateModel) paramList.get(2))).toString();
            String unit = FreemarkerUtils.getObject(((TemplateModel) paramList.get(3))).toString();
            try {
                return DateTimeUtils.add(pattern, DateTimeUtils.parse(dateStr, pattern), Integer.parseInt(amount), unit);
            } catch (ParseException e) {
                throw new MessageRuntimeException("日期格式不正确");
            }
        }
        throw new MessageRuntimeException("参数个数不正确");
    }

}