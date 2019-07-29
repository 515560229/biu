package com.abc.util.kafka;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.MessageAndOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class KafkaProducerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerUtils.class);

    private static final String TOPIC = "test"; //kafka创建的topic
    private static final String CONTENT = "This is a single message"; //要发送的内容
    private static final String BROKER_LIST = "localhost:9092"; //broker的地址和端口
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final String CLIENT_ID = "consumer_test_id";
    private static final String SERIALIZER_CLASS = "kafka.serializer.StringEncoder"; // 序列化类
    private static final int BUFFER_SIZE = 100 * 1024 * 1024; // SimpleConsumer-buffer_size-100MB

    /**
     * 构造连接参数
     * https://www.cnblogs.com/fxjwind/p/3956353.html
     *
     * @return
     */
    private static ConsumerConfig buildProperties() {
        Properties props = new Properties();
        props.put("zookeeper.connect", ZOOKEEPER_ADDRESS);
        props.put("group.id", "test_group");

        props.put("zookeeper.session.timeout.ms", "10000");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        props.put("auto.offset.reset", "smallest");//largest,smallest
        props.put("client.id", CLIENT_ID);
        return new ConsumerConfig(props);
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        SimpleConsumer simpleConsumer = new SimpleConsumer("localhost", 9092, 100000, BUFFER_SIZE, CLIENT_ID);

        //获取分区数
        for (int i = 0; i < 3; i++) {
            try {
                List<TopicMetadata> topicMetadataList = simpleConsumer.send(new TopicMetadataRequest(Arrays.asList(new String[]{TOPIC}))).topicsMetadata();
                TopicMetadata topicMetadata = topicMetadataList.get(0);
                LOG.info("fetch topic: {} errorCode: {}", TOPIC, topicMetadata.errorCode());
                List<PartitionMetadata> partitionMetadataList = topicMetadata.partitionsMetadata();
                for (PartitionMetadata partitionMetadata : partitionMetadataList) {
                    LOG.info("fetch topic: {} partition:{} errorCode: {}", TOPIC,
                            partitionMetadata.partitionId(), partitionMetadata.errorCode());
                }
            } catch (Exception e) {
                try {
                    Thread.sleep((long) ((i + Math.random()) * 1000));
                } catch (InterruptedException e2) {
                    LOG.warn("Caught InterruptedException: " + e2);
                }
            }
        }


        //获取消息
        FetchRequest req = new FetchRequestBuilder().clientId(CLIENT_ID)
                .addFetch(TOPIC, 0, 0L, 1000000)
                .addFetch(TOPIC, 1, 0L, 1000000)
                .addFetch(TOPIC, 2, 0L, 1000000)
                .addFetch(TOPIC, 3, 0L, 1000000)
                .build();
        FetchResponse fetchResponse = simpleConsumer.fetch(req);
        ByteBufferMessageSet messageSet = fetchResponse.messageSet(TOPIC, 3);
        for (MessageAndOffset messageAndOffset : messageSet) {
            ByteBuffer payload = messageAndOffset.message().payload();
            long offset = messageAndOffset.offset();
            byte[] bytes = new byte[payload.limit()];
            payload.get(bytes);
            System.out.println("Offset:" + offset + ", Payload:" + new String(bytes, "UTF-8"));
        }
    }
}
