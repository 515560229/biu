package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import freemarker.template.TemplateModel;

import java.util.List;
import java.util.Random;

public class RandomIntFunction extends CustomFunction {

    private static final String FUNCTION_NAME = "randomInt";
    private static final String DESC = String.format("函数%s格式:${%s(%s)}", FUNCTION_NAME, FUNCTION_NAME, 5);

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
        if (paramList.isEmpty()) {
            return String.valueOf(new Random().nextInt());
        }
        int firstParameter = Integer.parseInt(
                FreemarkerUtils.getObject((TemplateModel) paramList.get(0)).toString());
        if (paramList.size() == 1) {
            //1个参数, 则生成x. 0<= x <= secondParameter
            return String.valueOf(new Random().nextInt(firstParameter + 1));
        }
        if (paramList.size() == 2) {
            //2个参数, 则生成x. firstParameter<= x <=secondParameter
            int secondParameter = Integer.parseInt(
                    FreemarkerUtils.getObject((TemplateModel) paramList.get(1)).toString());
            return String.valueOf(firstParameter + new Random().nextInt(secondParameter - firstParameter + 1));
        }
        throw new MessageRuntimeException("参数个数不支持");
    }
}
