package com.abc.util.kafka;


import com.abc.vo.commonconfigvoproperty.KafkaProducerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Kafka08Producer implements KafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka08Producer.class);

    private final org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> producer;
    private final KafkaProducerConfig producerConfig;

    public Kafka08Producer(KafkaProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerConfig.getBroker());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "__BIU__");
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        producer = new org.apache.kafka.clients.producer.KafkaProducer(props);
    }

    public boolean send() {
        ProducerRecord<String, byte[]> producerRecord = null;
        try {
            producerRecord = new ProducerRecord(producerConfig.getTopic(), null, producerConfig.getMessage().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        Future<RecordMetadata> future = producer.send(producerRecord);
        RecordMetadata meta = null;
        try {
            meta = future.get(30, TimeUnit.SECONDS);
            logger.info("cluster: {},topic:{} send success. partition:{},offset: {}", producerConfig.getClusterName(), meta.topic(), meta.partition(), meta.offset());
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("发送失败。cluster: {},topic:{},message:{}", producerConfig.getClusterName(), producerConfig.getTopic(), producerConfig.getMessage());
            return false;
        } finally {
            producer.close();
        }
    }

    public static void main(String[] args) {
        KafkaProducerConfig producerConfig = new KafkaProducerConfig();
        producerConfig.setBroker("localhost:9092");
        producerConfig.setTopic("test");
        producerConfig.setMessage(UUID.randomUUID().toString());
        Kafka08Producer producer = new Kafka08Producer(producerConfig);
        producer.send();
    }

}
