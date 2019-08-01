package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaTopicConfig;
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

public class Kafka11Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka11Consumer.class);
    private static final String GROUP_ID = "__BIU__";
    private static final int WAIT_MAX_SECONDS = 30;
    private static final int MAX_MESSAGE_COUNT = 200;
    private final Map<String, KafkaMessage> messages = new ConcurrentHashMap<>();
    private KafkaTopicConfig clusterConfig;
    private long start;
    private long cost;
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;
    @Getter
    private AtomicLong fetchCount = new AtomicLong(0);
    @Getter
    private AtomicLong totalCount = new AtomicLong(0);

    public Kafka11Consumer(KafkaTopicConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
        start = System.currentTimeMillis();
        //构建properties
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, clusterConfig.getBroker());
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

        consume();

        kafkaConsumer.close();
    }

    private boolean match(String message) {
        if (StringUtils.isBlank(clusterConfig.getKeyword())) {
            return true;
        }
        if (message.contains(clusterConfig.getKeyword())) {
            return true;
        }
        return false;
    }

    protected String getMessageKey(int partition, long offset) {
        return String.format("%s-%s", partition, offset);
    }

    protected String toString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return e.getMessage();
        }
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

            List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(clusterConfig.getTopic());

            Collection<TopicPartition> topicPartitions = new ArrayList<>();
            for (PartitionInfo partitionInfo : partitionInfos) {
                topicPartitions.add(new TopicPartition(clusterConfig.getTopic(), partitionInfo.partition()));
            }
            kafkaConsumer.assign(topicPartitions);
            kafkaConsumer.seekToBeginning(topicPartitions);

            ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(10000);

            logger.info("fetch times: {}. count: {}", fetchCount.get(), records.count());
            for (ConsumerRecord<byte[], byte[]> record : records) {
                totalCount.incrementAndGet();
                String message = toString(record.value());
                if (match(message)) {
                    messages.put(getMessageKey(record.partition(), record.offset()),
                            new KafkaMessage(toString(record.key()), message, headers(record.headers())));
                }
            }
            if (messages.size() >= MAX_MESSAGE_COUNT || records.isEmpty() || (System.currentTimeMillis() - start) > WAIT_MAX_SECONDS * 1000) {
                break;
            }
        }
        cost = System.currentTimeMillis() - start;
        logger.info("finish and close for topic {} {}", clusterConfig.getClusterName(), clusterConfig.getTopic());
    }

    public Map<String, KafkaMessage> getMessages() {
        return this.messages;
    }

    public long getCost() {
        return this.cost;
    }

    public static void main(String[] args) {
        KafkaTopicConfig kafkaTopicConfig = new KafkaTopicConfig();
        kafkaTopicConfig.setBroker("inc-sgs-kafka-01.intsit.sfdc.com.cn:9092," +
                "inc-sgs-kafka-02.intsit.sfdc.com.cn:9092," +
                "inc-sgs-kafka-03.intsit.sfdc.com.cn:9092," +
                "inc-sgs-kafka-04.intsit.sfdc.com.cn:9092," +
                "inc-sgs-kafka-05.intsit.sfdc.com.cn:9092");
        kafkaTopicConfig.setClusterName("testCluster");
        // DIS.DELIVERY.ORDER.PICK.OMS.OPERATION.SGS-KAFKA-GW.ENV3-2
        // DIS.DELIVERY.ORDER.PIS.OMPS.TIME.SGS-KAFKA-GW.ENV3-2
        kafkaTopicConfig.setTopic("DIS.DELIVERY.ORDER.PICK.OMS.OPERATION.SGS-KAFKA-GW.ENV3-2");
        Kafka11Consumer consumer = new Kafka11Consumer(kafkaTopicConfig);
        Map<String, KafkaMessage> messages = consumer.getMessages();
        logger.info("cost: {}, messages: {}", consumer.getCost(), JSON.toJSONString(messages));
        logger.info("fetchCount: {} totalCount: {} cost: {}, message size: {} messages: {}", consumer.fetchCount.get(), consumer.getTotalCount().get(), consumer.getCost(), messages.size(), JSON.toJSONString(messages));
    }

}
