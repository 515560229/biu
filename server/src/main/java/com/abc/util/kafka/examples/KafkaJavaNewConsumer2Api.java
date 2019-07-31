package com.abc.util.kafka.examples;

import com.alibaba.fastjson.JSON;
import org.apache.kafka110.clients.consumer.ConsumerConfig;
import org.apache.kafka110.clients.consumer.ConsumerRecord;
import org.apache.kafka110.clients.consumer.ConsumerRecords;
import org.apache.kafka110.clients.consumer.KafkaConsumer;
import org.apache.kafka110.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;

// 0.8的时候一直报错, 循环
public class KafkaJavaNewConsumer2Api extends Thread {
    private KafkaConsumer consumer;
    private static int pullMaxSize = 10;
    private static final Logger logger = LoggerFactory.getLogger(KafkaJavaNewConsumer2Api.class);
    private List<String> messages = new ArrayList<>();

    public KafkaJavaNewConsumer2Api() {
        consumer = createConsumer();
        consumer.subscribe(Arrays.asList("test"));
    }

    private static KafkaConsumer createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024 * 5);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000 * 60);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "_myGroup100_");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "_myGroup100_");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

//        props.put("sasl.mechanism", "PLAIN");
//        props.put("auto.offset.reset", "earliest");//0.8

        return new KafkaConsumer<>(props);
    }

    @Override
    public void run() {
        int count = 1;
        int seconds = 1;
        while (true) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(100);
            for (ConsumerRecord<byte[], byte[]> cecord : records) {
                try {
                    messages.add(new String(cecord.value(), "UTF-8"));
                    Thread.sleep(1000 * 1);
                } catch (UnsupportedEncodingException e) {
                    logger.error("encoding error.", e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            seconds++;
            count += records.count();
            if (count >= pullMaxSize || seconds > 5) {
                logger.info("pull break. count={}. messageSize: {}", count, messages.size());
                consumer.close();
                break;
            }
        }
        logger.info("messages: {}", JSON.toJSONString(messages));
    }

    public static void main(String[] args) {
        KafkaJavaNewConsumer2Api consumer2Api = new KafkaJavaNewConsumer2Api();
        consumer2Api.start();
    }

}
