package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaTopicConfig;
import com.alibaba.fastjson.JSON;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;
import kafka.utils.VerifiableProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Kafka08Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka08Consumer.class);
    private static final String GROUP_ID = "__BIU__";
    private static final int consumeHandleThreads = 2;
    private static final int WAIT_MAX_SECONDS = 30;
    private static final int MAX_MESSAGE_COUNT = 200;
    private final ConsumerConnector consumer;
    private final Map<String, KafkaMessage> messages = new ConcurrentHashMap<>();
    private KafkaTopicConfig clusterConfig;
    private long start;
    private long cost;

    public Kafka08Consumer(KafkaTopicConfig clusterConfig) throws InterruptedException {
        this.clusterConfig = clusterConfig;
        start = System.currentTimeMillis();
        Properties originalProps = new Properties();
        originalProps.put("zookeeper.connect", clusterConfig.getZkConnect());
        originalProps.put("auto.commit.enable", "false");
        originalProps.put("fetch.message.max.bytes", String.valueOf(1024 * 1024 * 10));
        //group 代表一个消费组
        originalProps.put("group.id", GROUP_ID);
        //zk连接超时时间
        originalProps.put("zookeeper.session.timeout.ms", "60000");
        //zk同步时间
        originalProps.put("zookeeper.sync.time.ms", "2000");
        //自动提交间隔时间
        originalProps.put("auto.commit.interval.ms", "1000");
        //消息日志自动偏移量,防止宕机后数据无法读取
        originalProps.put("auto.offset.reset", "smallest");
        originalProps.put("connections.max.idle.ms", "600000");
        //构建consumer connection 对象
        consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(originalProps));
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

    public void consume() throws InterruptedException {
        //指定需要订阅的topic
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(clusterConfig.getTopic(), new Integer(1));//线程数
        //指定key的编码格式
        Decoder<String> keyDecoder = new kafka.serializer.StringDecoder(new VerifiableProperties());
        //指定value的编码格式
        Decoder<String> valueDecoder = new kafka.serializer.StringDecoder(new VerifiableProperties());
        //获取topic 和 接受到的stream 集合
        Map<String, List<KafkaStream<String, String>>> map = consumer.createMessageStreams(topicCountMap, keyDecoder, valueDecoder);

        //根据指定的topic 获取 stream 集合
        List<KafkaStream<String, String>> kafkaStreams = map.get(clusterConfig.getTopic());
        ExecutorService executor = Executors.newFixedThreadPool(consumeHandleThreads);

        //因为是多个 message组成 message set ， 所以要对stream 进行拆解遍历
        for (final KafkaStream<String, String> kafkaStream : kafkaStreams) {
            //拆解每个的 stream
            executor.submit(() ->
                    {
                        ConsumerIterator<String, String> iterator = kafkaStream.iterator();
                        while (iterator.hasNext()) {
                            MessageAndMetadata<String, String> messageAndMetadata = iterator.next();
                            if (match(messageAndMetadata.message())) {
                                messages.put(String.format("%s-%s", messageAndMetadata.partition(), messageAndMetadata.offset()),
                                        new KafkaMessage(messageAndMetadata.key(), messageAndMetadata.message(), null));
                            }
                        }
                    }
            );
        }
        int waitSeconds = 1;
        while (true) {
            if (messages.size() >= MAX_MESSAGE_COUNT || waitSeconds > WAIT_MAX_SECONDS) {
                break;
            }
            waitSeconds++;
            Thread.sleep(1000 * 1);
        }
        consumer.shutdown();
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000 * 1);
                logger.info("wait executor terminated for topic {} {}", clusterConfig.getClusterName(), clusterConfig.getTopic());
            } catch (InterruptedException e) {
                logger.error("中断失败", e);
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

    public static void main(String[] args) throws InterruptedException {
        KafkaTopicConfig kafkaTopicConfig = new KafkaTopicConfig();
        kafkaTopicConfig.setZkConnect("localhost:12181");
        kafkaTopicConfig.setClusterName("test");
        kafkaTopicConfig.setTopic("test");
        Kafka08Consumer kafka08Consumer = new Kafka08Consumer(kafkaTopicConfig);
        Map<String, KafkaMessage> messages = kafka08Consumer.getMessages();
        logger.info("cost: {}, messages: {}", kafka08Consumer.getCost(), JSON.toJSONString(messages));
    }

}
