package com.abc.util.freemarker;

import java.util.List;

public class BlankStringFunction extends CustomFunction {
    private static final String FUNCTION_NAME = "blank";

    private static final String DESC = String.format("函数%s格式:${%s()}}", FUNCTION_NAME, FUNCTION_NAME);

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getFunctionDesc() {
        return DESC;
    }

    /**
     * ${blank()} return ""
     *
     * @param paramList
     * @return
     */
    @Override
    public String execFunction(List paramList) {
        return "";
    }
}
