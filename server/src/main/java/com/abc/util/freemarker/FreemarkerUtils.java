package com.abc.util.freemarker;

import com.abc.exception.MessageRuntimeException;
import freemarker.template.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class FreemarkerUtils {
    public static final FreemarkerUtils INSTANCE = new FreemarkerUtils();

    private Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);

    private FreemarkerUtils() {
        cfg.setDefaultEncoding("UTF-8");
        Arrays.asList(new NowFunction()
                , new UUIDFunction()
                , new RandomIntFunction()
                , new BlankStringFunction()
                , new GetVarFunction())
                .forEach(fun -> cfg.setSharedVariable(fun.getFunctionName(), fun));
    }

    public String render(String stringTemplate, Map<String, Object> params) throws IOException, TemplateException {
        Template template = new Template(UUID.randomUUID().toString(), new StringReader(stringTemplate), cfg);
        StringWriter writer = new StringWriter();
        template.process(params, writer);
        return writer.toString();
    }

    static Object getObject(TemplateModel model) {
        if (model instanceof SimpleNumber) {
            // Number
            return ((SimpleNumber) model).getAsNumber();
        }
        if (model instanceof SimpleDate) {
            //DateTime
            return ((SimpleDate) model).getAsDate();
        }
        if (model instanceof SimpleScalar) {
            //String
            return ((SimpleScalar) model).getAsString();
        }
        if (model instanceof DefaultArrayAdapter) {
            return ((DefaultArrayAdapter) model).getWrappedObject();
        }
        if (model instanceof DefaultListAdapter) {
            return ((DefaultListAdapter) model).getWrappedObject();
        }
        throw new MessageRuntimeException("参数格式不正确");
    }

    public static void main(String[] args) throws IOException, TemplateException {
        String template = "uuid()\t${uuid()}\n" +
                "now()\t${now()}\n" +
                "now('yyyy-MM-dd HH:mm:ss')\t${now('yyyy-MM-dd HH:mm:ss')}\n" +
                "randomInt()\t${randomInt()}\n" +
                "randomInt(5)\t${randomInt(5)}\n" +
                "randomInt(2,5)\t${randomInt(2,5)}";
        String result = FreemarkerUtils.INSTANCE.render(template, Collections.emptyMap());
        System.out.println(result);
    }
}
