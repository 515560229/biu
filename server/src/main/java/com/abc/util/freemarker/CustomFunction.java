package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import freemarker.template.TemplateMethodModelEx;

import java.util.List;

public abstract class CustomFunction implements TemplateMethodModelEx {

    public abstract String getFunctionName();

    public abstract String getFunctionDesc();

    public abstract String execFunction(List paramList);

    @Override
    public String exec(List arguments) {
        try {
            return execFunction(arguments);
        } catch (Exception ex) {
            throw new MessageRuntimeException(getFunctionDesc(), ex);
        }
    }
}
