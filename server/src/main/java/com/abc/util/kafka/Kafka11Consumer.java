package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka110.clients.consumer.ConsumerConfig;
import org.apache.kafka110.clients.consumer.ConsumerRecord;
import org.apache.kafka110.clients.consumer.ConsumerRecords;
import org.apache.kafka110.clients.consumer.KafkaConsumer;
import org.apache.kafka110.common.PartitionInfo;
import org.apache.kafka110.common.TopicPartition;
import org.apache.kafka110.common.header.Header;
import org.apache.kafka110.common.header.Headers;
import org.apache.kafka110.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Kafka11Consumer extends com.abc.util.kafka.KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka11Consumer.class);

    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;

    public Kafka11Consumer(KafkaConsumerConfig kafkaConsumerConfig) {
        super(kafkaConsumerConfig);
        //构建properties
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerConfig.getBroker());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000 * 30);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000 * 40);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 1000 * 10);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        kafkaConsumer = new KafkaConsumer<>(props);
    }

    protected List<Map<String, Object>> headers(Headers headers) {
        if (headers == null) {
            return null;
        }
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Header header : headers) {
            HashMap<String, Object> hashMap = Maps.newHashMap();
            hashMap.put(header.key(), toString(header.value()));
            resultList.add(hashMap);
        }
        if (resultList.isEmpty()) {
            return null;
        }
        return resultList;
    }

    public void consume() {
        while (true) {
            fetchCount.incrementAndGet();

            List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(kafkaConsumerConfig.getTopic());

            Collection<TopicPartition> topicPartitions = new ArrayList<>();
            for (PartitionInfo partitionInfo : partitionInfos) {
                topicPartitions.add(new TopicPartition(kafkaConsumerConfig.getTopic(), partitionInfo.partition()));
            }
            kafkaConsumer.assign(topicPartitions);
            kafkaConsumer.seekToBeginning(topicPartitions);

            ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(500);

            logger.info("fetch times: {}. count: {}", fetchCount.get(), records.count());
            for (ConsumerRecord<byte[], byte[]> record : records) {
                totalCount.incrementAndGet();
                String message = toString(record.value());
                if (match(message)) {
                    messages.put(getMessageKey(record.partition(), record.offset()),
                            new KafkaMessage(record.partition(), record.offset(), toString(record.key()), message, headers(record.headers())));
                }
            }
            if (messages.size() >= MAX_MESSAGE_COUNT || records.isEmpty() || (System.currentTimeMillis() - start) > WAIT_MAX_SECONDS * 1000) {
                break;
            }
        }
        kafkaConsumer.close();
        cost = System.currentTimeMillis() - start;
        logger.info("finish and close for topic {} {}", kafkaConsumerConfig.getClusterName(), kafkaConsumerConfig.getTopic());
    }

    public static void main(String[] args) throws InterruptedException {
        KafkaConsumerConfig kafkaConsumerConfig = new KafkaConsumerConfig();
//        kafkaConsumerConfig.setBroker("inc-sgs-kafka-01.intsit.sfdc.com.cn:9092," +
//                "inc-sgs-kafka-02.intsit.sfdc.com.cn:9092," +
//                "inc-sgs-kafka-03.intsit.sfdc.com.cn:9092," +
//                "inc-sgs-kafka-04.intsit.sfdc.com.cn:9092," +
//                "inc-sgs-kafka-05.intsit.sfdc.com.cn:9092");
//        kafkaConsumerConfig.setClusterName("testCluster");
//        // DIS.DELIVERY.ORDER.PICK.OMS.OPERATION.SGS-KAFKA-GW.ENV3-2
//        // DIS.DELIVERY.ORDER.PIS.OMPS.TIME.SGS-KAFKA-GW.ENV3-2
//        kafkaConsumerConfig.setTopic("DIS.DELIVERY.ORDER.PICK.OMS.OPERATION.SGS-KAFKA-GW.ENV3-2");

        //local env
        kafkaConsumerConfig.setBroker("localhost:19092");
        kafkaConsumerConfig.setClusterName("testCluster");
        kafkaConsumerConfig.setTopic("test1");

        com.abc.util.kafka.KafkaConsumer consumer = new Kafka11Consumer(kafkaConsumerConfig);
        consumer.consume();
        Map<String, KafkaMessage> messages = consumer.getMessages();

        logger.info("cost: {}, messages: {}", consumer.getCost(), JSON.toJSONString(messages));
        logger.info("fetchCount: {} totalCount: {} cost: {}, message size: {} messages: {}", consumer.fetchCount.get(), consumer.getTotalCount().get(), consumer.getCost(), messages.size(), JSON.toJSONString(messages));
    }

}
