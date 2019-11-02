package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import com.google.common.collect.Maps;
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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Kafka11Consumer extends com.abc.util.kafka.KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka11Consumer.class);

    public Kafka11Consumer(KafkaConsumerConfig kafkaConsumerConfig) {
        super(kafkaConsumerConfig);
    }

    private Properties buildProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerConfig.getBroker());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, Integer.MAX_VALUE);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000 * 3);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000 * 3);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 1000 * 10);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
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
        KafkaConsumer<byte[], byte[]> kafkaConsumer = new KafkaConsumer<>(buildProperties());
        List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(kafkaConsumerConfig.getTopic());

        Collection<TopicPartition> topicPartitions = new ArrayList<>();
        for (PartitionInfo partitionInfo : partitionInfos) {
            topicPartitions.add(new TopicPartition(kafkaConsumerConfig.getTopic(), partitionInfo.partition()));
        }

        Map<Integer, Long> beginningOffsetsByPartitionIdx = new HashMap<>();
        Map<TopicPartition, Long> beginningOffsets = kafkaConsumer.beginningOffsets(topicPartitions);
        for (Map.Entry<TopicPartition, Long> entry : beginningOffsets.entrySet()) {
            beginningOffsetsByPartitionIdx.put((entry.getKey()).partition(), entry.getValue());
        }

        Map<Integer, Long> endOffsetsByPartitionIdx = new HashMap<>();
        Map<TopicPartition, Long> endOffsets = kafkaConsumer.endOffsets(topicPartitions);
        for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
            endOffsetsByPartitionIdx.put((entry.getKey()).partition(), entry.getValue());
        }

        Map<Integer, KafkaOffsetPage> topicPartitionMap = new HashMap<>();
        for (PartitionInfo partitionInfo : partitionInfos) {
            KafkaOffsetPage kafkaOffsetPage = new KafkaOffsetPage(beginningOffsetsByPartitionIdx.get(partitionInfo.partition()),
                    endOffsetsByPartitionIdx.get(partitionInfo.partition()), 400);
            kafkaOffsetPage.setPartitionIdx(partitionInfo.partition());
            topicPartitionMap.put(partitionInfo.partition(), kafkaOffsetPage);
        }
        kafkaConsumer.close();

        /**
         * 1. 新建线程池处理,一个分区一个线程
         * 2. 获取该分区的beginningOffset and endOffset
         * 3. 所有的数据需要while pool
         */
        ExecutorService executor = Executors.newFixedThreadPool(1);
        for (TopicPartition topicPartition : topicPartitions) {
            executor.submit(() -> {
                KafkaOffsetPage kafkaOffsetPage = topicPartitionMap.get(topicPartition.partition());

                Properties executorProperties = buildProperties();
                executorProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID + "-" + topicPartition.partition());
                executorProperties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, GROUP_ID + "-" + topicPartition.partition());
                KafkaConsumer<byte[], byte[]> executorConsumer = new KafkaConsumer<>(executorProperties);
                logger.info("fetch topic {} partition {} offset {}-{}.total:{}", kafkaConsumerConfig.getTopic(), topicPartition.partition(),
                        kafkaOffsetPage.getOffsetStart(), kafkaOffsetPage.getOffsetEnd(), kafkaOffsetPage.getOffsetEnd() - kafkaOffsetPage.getOffsetStart());
                executorConsumer.assign(Arrays.asList(topicPartition));
                executorConsumer.seek(topicPartition, kafkaOffsetPage.getOffsetStart());

                while (true) {
                    long fetchCountValue = fetchCountIncrementAndGet();
                    ConsumerRecords<byte[], byte[]> records = executorConsumer.poll(500);
                    logger.info("fetch times: {}. count: {}.partition: {}", fetchCountValue, records.count(), topicPartition.partition());
                    if (records.count() == 0) {
                        break;
                    }
                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        totalCount.incrementAndGet();
                        String message = toString(record.value());
                        if (match(message)) {
                            messages.put(getMessageKey(record.partition(), record.offset()),
                                    new KafkaMessage(record.partition(), record.offset(), toString(record.key()), message, headers(record.headers())));
                        }
                    }
                }
                executorConsumer.close();
                //判断是否需要退出
                int messageSize = messages.size();
                if (messageSize >= MAX_MESSAGE_COUNT || (System.currentTimeMillis() - start) / 1000 > WAIT_MAX_SECONDS) {
                    logger.info("partition: {} fetch break. messageSize:{}", topicPartition.partition(), messageSize);
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000 * 1);
                logger.info("wait executor terminated for topic {} {}", kafkaConsumerConfig.getClusterName(), kafkaConsumerConfig.getTopic());
            } catch (InterruptedException e) {
                logger.error("中断失败", e);
            }
        }

        cost = System.currentTimeMillis() - start;
        logger.info("finish and close for topic {} {}", kafkaConsumerConfig.getClusterName(), kafkaConsumerConfig.getTopic());
    }
}
