package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaTopicConfig;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka110.clients.consumer.ConsumerConfig;
import org.apache.kafka110.clients.consumer.ConsumerRecord;
import org.apache.kafka110.clients.consumer.ConsumerRecords;
import org.apache.kafka110.clients.consumer.KafkaConsumer;
import org.apache.kafka110.common.header.Header;
import org.apache.kafka110.common.header.Headers;
import org.apache.kafka110.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Kafka11Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka11Consumer.class);
    private static final String GROUP_ID = "__BIU__";
    private static final int consumeHandleThreads = 1;
    private static final int WAIT_MAX_SECONDS = 30;
    private static final int MAX_MESSAGE_COUNT = 200;
    private final Map<String, KafkaMessage> messages = new ConcurrentHashMap<>();
    private KafkaTopicConfig clusterConfig;
    private long start;
    private long cost;
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;

    public Kafka11Consumer(KafkaTopicConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
        start = System.currentTimeMillis();
        //构建properties
        Properties props = new Properties();
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, clusterConfig.getBroker());
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024 * 5);
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000 * 60);
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(org.apache.kafka110.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Arrays.asList(clusterConfig.getTopic()));

        consume();
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
            int count = 1;
            ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(500);
            logger.info("poll count: {}", count);
            for (ConsumerRecord<byte[], byte[]> record : records) {
                String message = toString(record.value());
                if (match(message)) {
                    messages.put(getMessageKey(record.partition(), record.offset()),
                            new KafkaMessage(toString(record.key()), message, headers(record.headers())));
                }
            }
            count++;
            if (messages.size() >= MAX_MESSAGE_COUNT || records.isEmpty() || (System.currentTimeMillis() - start) > WAIT_MAX_SECONDS * 1000) {
                break;
            }
        }
        cost = System.currentTimeMillis() - start;
        logger.info("finish and close for topic {} {}", clusterConfig.getClusterName(), clusterConfig.getTopic());
        kafkaConsumer.close();
    }

    public Map<String, KafkaMessage> getMessages() {
        return this.messages;
    }

    public long getCost() {
        return this.cost;
    }

    public static void main(String[] args) {
        KafkaTopicConfig kafkaTopicConfig = new KafkaTopicConfig();
        kafkaTopicConfig.setBroker("localhost:19092");
        kafkaTopicConfig.setClusterName("testCluster");
        kafkaTopicConfig.setTopic("test");
        Kafka11Consumer consumer = new Kafka11Consumer(kafkaTopicConfig);
        Map<String, KafkaMessage> messages = consumer.getMessages();
        logger.info("cost: {}, messages: {}", consumer.getCost(), JSON.toJSONString(messages));
    }

}
