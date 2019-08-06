package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.schedule.FetchTopicMetaDataScheduler;
import com.abc.util.freemarker.FreemarkerUtils;
import com.abc.util.kafka.*;
import com.abc.vo.CommonConfigVo;
import com.abc.vo.Json;
import com.abc.vo.commonconfigvoproperty.KafkaClusterConfig;
import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import com.abc.vo.commonconfigvoproperty.KafkaProducerConfig;
import com.abc.vo.commonconfigvoproperty.Parameter;
import freemarker.template.TemplateException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@PermInfo(value = "Kafka模块", pval = "a:kafka:接口")
@RestController
@RequestMapping("/kafka/executor")
public class KafkaController {
    public static final String VERSION_08 = "0.8";
    public static final String VERSION_11 = "1.1";

    @Autowired
    private FetchTopicMetaDataScheduler fetchTopicMetaDataScheduler;

    @PermInfo("主题内容搜索")
    @RequiresPermissions("a:kafka:consumer")
    @PostMapping(value = "/consumer")
    public Json consumer(@RequestBody CommonConfigVo commonConfigVo) throws InvocationTargetException, IllegalAccessException, IOException, TemplateException {
        String oper = "kafkaConsumerExecute";
        KafkaConsumerConfig consumerConfig = commonConfigVo.getConsumerConfig();

        //通过主题名称, 找到集群配置
        String topic = consumerConfig.getTopic();
        int splitIndex = topic.indexOf('.');
        String cluster = topic.substring(0, splitIndex);
        String realTopicName = topic.substring(splitIndex + 1);

        KafkaClusterConfig kafkaClusterConfig = fetchTopicMetaDataScheduler.getByClusterName(cluster);
        BeanUtils.copyProperties(consumerConfig, kafkaClusterConfig);
        consumerConfig.setTopic(realTopicName);

        Map<String, Object> parameterMap = getParameterMap(consumerConfig.getParameters());
        consumerConfig.setKeyword(FreemarkerUtils.INSTANCE.render(consumerConfig.getKeyword(), parameterMap));//变量处理

        Map<String, Object> responseObj = new HashMap<>();
        KafkaConsumer kafkaConsumer = null;
        if (VERSION_08.equals(consumerConfig.getVersion())) {
            kafkaConsumer = new Kafka08OldConsumer(consumerConfig);
        } else {
            kafkaConsumer = new Kafka11Consumer(consumerConfig);
        }
        kafkaConsumer.consume();
        Map<String, KafkaMessage> messages = kafkaConsumer.getMessages();
        AtomicLong totalCount = kafkaConsumer.getTotalCount();
        responseObj.put("totalCount", totalCount.get());
        responseObj.put("messages", messages.values());
        return Json.succ(oper, responseObj);
    }

    @PermInfo("主题消息发送")
    @RequiresPermissions("a:kafka:producer")
    @PostMapping(value = "/producer")
    public Json producer(@RequestBody CommonConfigVo commonConfigVo) throws InvocationTargetException, IllegalAccessException, IOException, TemplateException {
        String oper = "kafkaProducerExecute";
        KafkaProducerConfig producerConfig = commonConfigVo.getProducerConfig();

        //通过主题名称, 找到集群配置
        String topic = producerConfig.getTopic();
        int splitIndex = topic.indexOf('.');
        String cluster = topic.substring(0, splitIndex);
        String realTopicName = topic.substring(splitIndex + 1);

        KafkaClusterConfig kafkaClusterConfig = fetchTopicMetaDataScheduler.getByClusterName(cluster);
        BeanUtils.copyProperties(producerConfig, kafkaClusterConfig);
        producerConfig.setTopic(realTopicName);

        Map<String, Object> parameterMap = getParameterMap(producerConfig.getParameters());
        producerConfig.setMessage(FreemarkerUtils.INSTANCE.render(producerConfig.getMessage(), parameterMap));//变量处理

        Map<String, Object> responseObj = new HashMap<>();
        KafkaProducer kafkaProducer = null;
        if (VERSION_08.equals(producerConfig.getVersion())) {
            kafkaProducer = new Kafka08Producer(producerConfig);
        } else {
            kafkaProducer = new Kafka11Producer(producerConfig);
        }
        boolean result = kafkaProducer.send();
        responseObj.put("result", result);
        responseObj.put("message", producerConfig.getMessage());
        return Json.succ(oper, responseObj);
    }

    private Map<String, Object> getParameterMap(List<Parameter> parameters) throws IOException, TemplateException {
        Map<String, Object> parameterMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(parameters)) {
            for (Parameter parameter : parameters) {
                //参数的值可能是函数
                parameterMap.put(parameter.getName(), FreemarkerUtils.INSTANCE.render(parameter.getDefaultValue(), Collections.emptyMap()));
            }
        }
        return parameterMap;
    }
}
