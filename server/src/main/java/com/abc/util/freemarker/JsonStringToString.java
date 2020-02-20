package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonStringToString extends CustomFunction {
    private static final String FUNCTION_NAME = "jsonStringToString";
    private static final String DESC = String.format("函数%s格式:${%s(%s)}", FUNCTION_NAME, FUNCTION_NAME, "{\"username\":\"zhangsan\"}", ",");

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
            throw new MessageRuntimeException("参数个数不能为空");
        }
        if (paramList.size() != 1) {
            throw new MessageRuntimeException("只支持一个参数");
        }
        String s = FreemarkerUtils.getObject(paramList.get(0)).toString();
        Map<String, String> map = new HashMap<>();
        map.put("value", s);
        String s1 = JSON.toJSONString(map);
        return s1.substring(10, s1.length() - 2);
    }
}
