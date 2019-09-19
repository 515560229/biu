package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

@Slf4j
public class Kafka08OldConsumerTest {

    @Test
    public void consume() {
        KafkaConsumerConfig kafkaConsumerConfig = new KafkaConsumerConfig();
        kafkaConsumerConfig.setBroker("10.202.24.5:9094");
        kafkaConsumerConfig.setClusterName("sit-other");
        kafkaConsumerConfig.setTopic("SGS_BIZLOG_DATA");
        kafkaConsumerConfig.setVersion("0.8");

        //local env
//        kafkaConsumerConfig.setBroker("localhost:19092");
//        kafkaConsumerConfig.setClusterName("testCluster");
//        kafkaConsumerConfig.setTopic("test1");

        kafkaConsumerConfig.setKeyword("31127 CX2019091865268466");
        com.abc.util.kafka.KafkaConsumer consumer = new Kafka08OldConsumer(kafkaConsumerConfig);
        consumer.consume();
        Map<String, KafkaMessage> messages = consumer.getMessages();

        log.info("fetchCount: {} totalCount: {} cost: {}, message size: {} messages: {}", consumer.getFetchCount(), consumer.getTotalCount().get(), consumer.getCost(), messages.size(),
                JSON.toJSONString(messages, SerializerFeature.PrettyFormat));
    }
}