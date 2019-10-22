package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaClusterConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class KafkaTopicFetcherTest {

    @Test
    public void fetch() {
        KafkaTopicFetcher fetcher = new KafkaTopicFetcher();
        KafkaClusterConfig clusterConfig = new KafkaClusterConfig();
        clusterConfig.setBroker("10.202.24.5:19097,10.202.24.5:9096,10.202.24.6:9096,10.202.24.7:9096");
        List<KafkaTopic> kafkaTopics = fetcher.fetch(clusterConfig);

        System.out.println(JSON.toJSONString(kafkaTopics, SerializerFeature.PrettyFormat));
        System.out.println(kafkaTopics.size());
    }
}