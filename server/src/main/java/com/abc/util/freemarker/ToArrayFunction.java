package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import com.alibaba.fastjson.JSON;
import freemarker.template.TemplateModel;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ToArrayFunction extends CustomFunction {
    private static final String SPLIT_STR = ",";
    private static final String FUNCTION_NAME = "toArray";
    private static final String DESC = String.format("函数%s格式:${%s(%s,%s)}", FUNCTION_NAME, FUNCTION_NAME, "1,2,3", ",");

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
        if (paramList.size() == 1) {
            //只有一个参数场景
            String s = FreemarkerUtils.getObject(paramList.get(0)).toString();
            String[] split = s.split(SPLIT_STR);
            List<String> arr = new ArrayList<>();
            for (String s1 : split) {
                arr.add(String.valueOf(s1));
            }
            String jsonString = JSON.toJSONString(arr);
            return jsonString.substring(1, jsonString.length() - 1);
        }
        if (paramList.size() == 2) {
            //有2个参数场景
            String s = FreemarkerUtils.getObject(paramList.get(0)).toString();
            String type = FreemarkerUtils.getObject(paramList.get(1)).toString();
            String[] split = s.split(SPLIT_STR);
            List<Object> arr = new ArrayList<>();
            for (String s1 : split) {
                if (type.equals("string")) {
                    arr.add(String.valueOf(s1));
                } else if (type.equals("number")) {
                    arr.add(NumberUtils.createNumber(s1));
                }
            }
            String jsonString = JSON.toJSONString(arr);
            return jsonString.substring(1, jsonString.length() - 1);
        }
        throw new MessageRuntimeException("参数个数不支持");
    }

    public static void main(String[] args) {
        System.out.println(new ToArrayFunction().execFunction(Arrays.asList("1.5,2,3", "number")));
    }
}
