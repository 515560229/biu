package com.abc.util.kafka.examples;

import org.apache.kafka110.clients.consumer.ConsumerConfig;
import org.apache.kafka110.clients.consumer.ConsumerRecord;
import org.apache.kafka110.clients.consumer.ConsumerRecords;
import org.apache.kafka110.clients.consumer.KafkaConsumer;
import org.apache.kafka110.common.TopicPartition;
import org.apache.kafka110.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

// 0.8的时候一直报错, 循环
public class KafkaJavaNewConsumerApi {
    private static final Logger logger = LoggerFactory.getLogger(KafkaJavaNewConsumerApi.class);

    private static final String FETCH_WARN = "fetch message in topic [{}-{}] from offset {} count: {}, actual count: {}";
    private static final String FETCH_TIME_WARN = "fetch topic: {}-{}, time: {} ms, count: {}, read records: {}";
    private static final String OFFSET_OUTOF_RANGE = "topic {}-{} offset out of range, request offset: {}, actual: {}-{}";
    private static final int READ_RETRY_COUNT = 3;
    private static final int READ_TIMEOUT = 3000;
    private static final int READ_WARNING_TIME = (int) (READ_TIMEOUT * 0.8);

    private static KafkaConsumer createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BOOTSTRAP_SERVER_11);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024 * 5);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000 * 60);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "_myGroup100_");

//        props.put("sasl.mechanism", "PLAIN");
//        props.put("auto.offset.reset", "earliest");//0.8

        return new KafkaConsumer<>(props);
    }

    public static void main(String[] args) {
        KafkaConsumer consumer = createConsumer();

        String topic = KafkaConstants.TOPIC_11;
        int partition = 0;
        int startOffset = KafkaConstants.OFFSET_11;
        int offset = startOffset;

        List<String> list = new ArrayList<>();
        TopicPartition tp = new TopicPartition(topic, partition);
        consumer.assign(Collections.singleton(tp));
        consumer.seek(tp, offset);
        long time = System.currentTimeMillis();

        int count = 100;//消费多少条
        if (count > 0) {
            int remainCount = count;
            int readCount = 0;
            while (remainCount > 0 && ++readCount <= READ_RETRY_COUNT) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(READ_TIMEOUT);
                // 检查是否offset超范围
                if (records.count() == 0 && readCount == 1) {
                    long beginOffset = (long) consumer.beginningOffsets(Collections.singleton(tp)).get(tp);
                    long endOffset = (long) consumer.endOffsets(Collections.singleton(tp)).get(tp);
                    if (offset < beginOffset || offset >= endOffset) {
                        logger.warn(OFFSET_OUTOF_RANGE, topic, partition, offset, beginOffset, endOffset);
                        break;
                    }
                }

                for (ConsumerRecord<byte[], byte[]> record : records) {
                    list.add(new String(record.value()));
                    if (--remainCount <= 0) {
                        break;
                    }
                }
            }

            if (remainCount > 0) {
                logger.warn(FETCH_WARN, topic, partition, offset, count, list.size());
            }
        } else {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(READ_TIMEOUT);
            records.forEach(record -> list.add(new String(record.value())));
        }

        time = System.currentTimeMillis() - time;
        if (time >= READ_WARNING_TIME) {
            logger.warn(FETCH_TIME_WARN, topic, partition, time, count, list.size());
        }

        logger.info("finish. size={}, {}", list.size(), list);
        consumer.close();

    }

}
