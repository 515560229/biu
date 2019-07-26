package com.abc.util.freemarker;

import freemarker.template.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class FreemarkerUtils {
    public static final FreemarkerUtils INSTANCE = new FreemarkerUtils();

    private Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);

    public FreemarkerUtils() {
        cfg.setDefaultEncoding("UTF-8");
        cfg.setSharedVariable("now", new Now());
    }

    public String render(String stringTemplate, Map<String, Object> params) throws IOException, TemplateException {
        Template template = new Template(UUID.randomUUID().toString(), new StringReader(stringTemplate), cfg);
        StringWriter writer = new StringWriter();
        template.process(params, writer);
        return writer.toString();
    }

    private static Object getObject(TemplateModel model){
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
            return ((DefaultArrayAdapter)model).getWrappedObject();
        }
        if (model instanceof DefaultListAdapter) {
            return (List<?>)((DefaultListAdapter)model).getWrappedObject();
        }
        throw new RuntimeException("参数格式不正确");
    }

    public static void main(String[] args) throws IOException, TemplateException {
        String template = "a ${b} c ${d} ${now('yyyy-MM-dd HH:mm:ss')}";
        Map<String, Object> params = new HashMap<>();
        params.put("b", "b");
        params.put("d", "d");

        String result = FreemarkerUtils.INSTANCE.render(template, params);
        System.out.println(result);
    }

    private static class Now implements TemplateMethodModelEx {
        @Override
        public Object exec(List paramList) {
            if (paramList.size() != 1) {
                throw new RuntimeException("now函数格式: ${now('yyyy-MM-dd HH:mm:ss')}");
            }
            Object format = paramList.get(0);
            if (!(format instanceof TemplateModel)) {
                throw new RuntimeException("now函数格式: ${now('yyyy-MM-dd HH:mm:ss')}");
            }
            return new SimpleDateFormat(getObject(((TemplateModel) format)).toString()).format(new Date());
        }
    }
}
