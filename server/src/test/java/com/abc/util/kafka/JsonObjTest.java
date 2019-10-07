package com.abc.util.kafka;

import com.alibaba.druid.support.json.JSONUtils;
import com.eviware.soapui.support.xml.XmlUtils;

import org.assertj.core.util.xml.XmlStringPrettyFormatter;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

public class JsonObjTest {
    @Test
    public void test1() {
        Map<String, Object> value = new HashMap<>();
        value.put("username", "zhangsan");

        Map<String, Object> value1 = new HashMap<>();
        value1.put("username", "lisi");
        value1.put("age", 10);

        Map<String, Object> value2 = new HashMap<>();
        value2.put("username", "wangwu");
        value2.put("age", 20);

        value1.put("friend", JSONUtils.toJSONString(value2));
        value.put("friend", JSONUtils.toJSONString(value1));

        System.out.println(JSONUtils.toJSONString(value));

    }

    @Test
    public void test2() throws Exception {
        Map<String, Object> value = new HashMap<>();
        value.put("username", "zhangsan");

        Map<String, Object> value1 = new HashMap<>();
        value1.put("username", "lisi");
        value1.put("age", 10);

        Map<String, Object> value2 = new HashMap<>();
        value2.put("username", "wangwu");
        value2.put("age", 20);

        value1.put("friend", JSONUtils.toJSONString(value2));
        value.put("friend", JSONUtils.toJSONString(value1));

        System.out.println(JsonObjTest.objectToXml(value));
    }

    /**
     * Object转XML
     *
     * @param object
     * @return
     * @throws Exception
     */
    public static String objectToXml(Object object) throws Exception {
        JAXBContext context = JAXBContext.newInstance(object.getClass());    // 获取上下文对象
        Marshaller marshaller = context.createMarshaller(); // 根据上下文获取marshaller对象
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");  // 设置编码字符集
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); // 格式化XML输出，有分行和缩进
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(object, baos);
        String xmlObj = new String(baos.toByteArray());         // 生成XML字符串
        return xmlObj.trim();
    }
}
