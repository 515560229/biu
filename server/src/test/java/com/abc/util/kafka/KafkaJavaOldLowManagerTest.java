package com.abc.util.kafka;

import com.abc.util.kafka.examples.KafkaConfig;
import com.abc.util.kafka.examples.KafkaJavaOldLowManager;
import com.abc.util.kafka.examples.KafkaOffsetRetrievalFailureException;
import com.alibaba.fastjson.JSON;
import kafka.message.MessageAndOffset;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;

public class KafkaJavaOldLowManagerTest {
    private static final Logger log = LoggerFactory.getLogger(KafkaJavaOldLowManagerTest.class);
    private KafkaJavaOldLowManager kafkaJavaOldLowManager = new KafkaJavaOldLowManager();

    @Test
    public void test1() throws KafkaOffsetRetrievalFailureException, UnsupportedEncodingException {
        String cluster = "exp08";
        String topic = "SHIVA_OMS_UNCALL_ACC_TO_SGS";

        KafkaConfig kafkaConfig = new KafkaConfig();
        Map<String, String> clusterMap = new HashMap<>();
        clusterMap.put(cluster, "10.202.24.5:9096,10.202.24.6:9096,10.202.24.7:9096,10.202.24.8:9096,10.202.24.9:9096");
        kafkaConfig.setCluster(clusterMap);
        kafkaJavaOldLowManager.setKafkaConfig(kafkaConfig);
        kafkaJavaOldLowManager.init();

        List<KafkaTopic> filteredTopics = kafkaJavaOldLowManager.getKafkaAPI().getFilteredTopics(cluster, Collections.emptyList(),
                Arrays.asList(Pattern.compile(topic)));

        log.info("topic: {} partition: {}", filteredTopics.get(0).getName(), filteredTopics.get(0).getPartitions().get(0).getId());
        long earliestOffset = kafkaJavaOldLowManager.getKafkaAPI().getEarliestOffset(filteredTopics.get(0).getPartitions().get(0));
        log.info("EarliestOffset: \t{}", earliestOffset);
        long latestOffset = kafkaJavaOldLowManager.getKafkaAPI().getLatestOffset(filteredTopics.get(0).getPartitions().get(0));
        log.info("getLatestOffset: \t{}", latestOffset);

        List<String> messages = new ArrayList<>();
        long lastOffset = earliestOffset;
        int times = 1;
        while (true) {
            Iterator<MessageAndOffset> bus = kafkaJavaOldLowManager.getKafkaAPI().fetchNextMessageBuffer(cluster, filteredTopics.get(0).getPartitions().get(0), lastOffset, latestOffset);
            log.info("times: {}", times);
            times++;
            while (bus.hasNext()) {
                MessageAndOffset messageAndOffset = bus.next();
                ByteBuffer payload = messageAndOffset.message().payload();
                byte[] bytes = new byte[payload.limit()];
                payload.get(bytes);
                messages.add(new String(bytes, "UTF-8"));
                lastOffset = messageAndOffset.offset();
                log.info("current offset: {}", messageAndOffset.offset());
            }
            lastOffset++;
            if (lastOffset >= latestOffset) {
                break;
            }
        }
        log.info("size: {}, messages: {}", messages.size(), JSON.toJSONString(messages));

        kafkaJavaOldLowManager.destroy();
    }

    @Test
    public void test2() throws KafkaOffsetRetrievalFailureException, UnsupportedEncodingException {
        String cluster = "exp08";
        String topic = "DELIVERY.SGS.ORDER.STATUS.UPLOAD.SGS-KAFKA-GW.ENV3-2";

        KafkaConfig kafkaConfig = new KafkaConfig();
        Map<String, String> clusterMap = new HashMap<>();
        clusterMap.put(cluster,
                "inc-sgs-kafka-02.intsit.sfdc.com.cn:9092,inc-sgs-kafka-03.intsit.sfdc.com.cn:9092,inc-sgs-kafka-04.intsit.sfdc.com.cn:9092,inc-sgs-kafka-05.intsit.sfdc.com.cn:9092");
        kafkaConfig.setCluster(clusterMap);
        kafkaJavaOldLowManager.setKafkaConfig(kafkaConfig);
        kafkaJavaOldLowManager.init();

        List<KafkaTopic> filteredTopics = kafkaJavaOldLowManager.getKafkaAPI().getFilteredTopics(cluster, Collections.emptyList(),
                Arrays.asList(Pattern.compile(topic)));

        log.info("topic: {} partition: {}", filteredTopics.get(0).getName(), filteredTopics.get(0).getPartitions().get(0).getId());
        long earliestOffset = kafkaJavaOldLowManager.getKafkaAPI().getEarliestOffset(filteredTopics.get(0).getPartitions().get(0));
        log.info("EarliestOffset: \t{}", earliestOffset);
        long latestOffset = kafkaJavaOldLowManager.getKafkaAPI().getLatestOffset(filteredTopics.get(0).getPartitions().get(0));
        log.info("getLatestOffset: \t{}", latestOffset);

        List<String> messages = new ArrayList<>();
        long lastOffset = earliestOffset;
        int times = 1;
        while (true) {
            Iterator<MessageAndOffset> bus = kafkaJavaOldLowManager.getKafkaAPI().fetchNextMessageBuffer(cluster, filteredTopics.get(0).getPartitions().get(0), lastOffset, latestOffset);
            log.info("times: {}", times);
            times++;
            while (bus.hasNext()) {
                MessageAndOffset messageAndOffset = bus.next();
                ByteBuffer payload = messageAndOffset.message().payload();
                byte[] bytes = new byte[payload.limit()];
                payload.get(bytes);
                messages.add(new String(bytes, "UTF-8"));
                lastOffset = messageAndOffset.offset();
                log.info("current offset: {}", messageAndOffset.offset());
            }
            lastOffset++;
            if (lastOffset >= latestOffset) {
                break;
            }
        }
        log.info("size: {}, messages: {}", messages.size(), JSON.toJSONString(messages));

        kafkaJavaOldLowManager.destroy();
    }
}