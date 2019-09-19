package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Map;

@Slf4j
public class Kafka11ConsumerTest {
    @Test
    public void test1() {
        KafkaConsumerConfig kafkaConsumerConfig = new KafkaConsumerConfig();
        kafkaConsumerConfig.setBroker("inc-sgs-kafka-01.intsit.sfdc.com.cn:9092"
                + ",inc-sgs-kafka-02.intsit.sfdc.com.cn:9092," +
                "inc-sgs-kafka-03.intsit.sfdc.com.cn:9092," +
                "inc-sgs-kafka-04.intsit.sfdc.com.cn:9092," +
                "inc-sgs-kafka-05.intsit.sfdc.com.cn:9092"
        );
        kafkaConsumerConfig.setClusterName("testCluster");
        // DIS.DELIVERY.ORDER.PICK.OMS.OPERATION.SGS-KAFKA-GW.ENV3-2
        // DIS.DELIVERY.ORDER.PIS.OMPS.TIME.SGS-KAFKA-GW.ENV3-2
        kafkaConsumerConfig.setTopic("DIS.RECEIVE.ORDER.OMS.SGS-KAFKA-GW.ENV3-1");

        //local env
//        kafkaConsumerConfig.setBroker("localhost:19092");
//        kafkaConsumerConfig.setClusterName("testCluster");
//        kafkaConsumerConfig.setTopic("test1");

        kafkaConsumerConfig.setKeyword("CX2019091815727702");
        com.abc.util.kafka.KafkaConsumer consumer = new Kafka11Consumer(kafkaConsumerConfig);
        consumer.consume();
        Map<String, KafkaMessage> messages = consumer.getMessages();

        log.info("cost: {}, messages: {}", consumer.getCost(), JSON.toJSONString(messages));
        log.info("fetchCount: {} totalCount: {} cost: {}, message size: {} messages: {}", consumer.getFetchCount(), consumer.getTotalCount().get(), consumer.getCost(), messages.size(), JSON.toJSONString(messages));
    }
}
