package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import com.alibaba.fastjson.JSON;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;
import kafka.utils.VerifiableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Kafka08Consumer extends KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka08Consumer.class);
    private static final int consumeHandleThreads = 2;
    private final ConsumerConnector consumer;

    public Kafka08Consumer(KafkaConsumerConfig clusterConfig) throws InterruptedException {
        super(clusterConfig);

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
    }

    @Override
    public void consume() {
        //指定需要订阅的topic
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(kafkaConsumerConfig.getTopic(), new Integer(1));//线程数
        //指定key的编码格式
        Decoder<String> keyDecoder = new kafka.serializer.StringDecoder(new VerifiableProperties());
        //指定value的编码格式
        Decoder<String> valueDecoder = new kafka.serializer.StringDecoder(new VerifiableProperties());
        //获取topic 和 接受到的stream 集合
        Map<String, List<KafkaStream<String, String>>> map = consumer.createMessageStreams(topicCountMap, keyDecoder, valueDecoder);

        //根据指定的topic 获取 stream 集合
        List<KafkaStream<String, String>> kafkaStreams = map.get(kafkaConsumerConfig.getTopic());
        ExecutorService executor = Executors.newFixedThreadPool(consumeHandleThreads);

        //因为是多个 message组成 message set ， 所以要对stream 进行拆解遍历
        for (final KafkaStream<String, String> kafkaStream : kafkaStreams) {
            //拆解每个的 stream
            executor.submit(() ->
                    {
                        ConsumerIterator<String, String> iterator = kafkaStream.iterator();
                        while (iterator.hasNext()) {
                            fetchCount.incrementAndGet();
                            if (messages.size() > MAX_MESSAGE_COUNT) {
                                break;
                            }
                            MessageAndMetadata<String, String> messageAndMetadata = iterator.next();
                            if (match(messageAndMetadata.message())) {
                                messages.put(getMessageKey(messageAndMetadata.partition(), messageAndMetadata.offset()),
                                        new KafkaMessage(messageAndMetadata.partition(), messageAndMetadata.offset(), messageAndMetadata.key(), messageAndMetadata.message(), null));
                            }
                        }
                    }
            );
        }
        int waitSeconds = 1;
        while (true) {
            int messageSize = messages.size();
            if (messageSize >= MAX_MESSAGE_COUNT || waitSeconds > WAIT_MAX_SECONDS) {
                logger.info("fetch break. messageSize:{}, waitSeconds: {}", messageSize, waitSeconds);
                break;
            }
            waitSeconds++;
            try {
                Thread.sleep(1000 * 1);
            } catch (InterruptedException e) {
                logger.error("中断失败", e);
            }
        }
        consumer.shutdown();
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

    public static void main(String[] args) throws InterruptedException {
        KafkaConsumerConfig kafkaConsumerConfig = new KafkaConsumerConfig();
        kafkaConsumerConfig.setZkConnect("10.202.24.5:2181/kafka/bus");
        kafkaConsumerConfig.setClusterName("bus");
        kafkaConsumerConfig.setTopic("SHIVA_OMS_UNCALL_ACC_TO_SGS");
        kafkaConsumerConfig.setKeyword("12201072216241391802552201");
        Kafka08Consumer kafka08Consumer = new Kafka08Consumer(kafkaConsumerConfig);
        Map<String, KafkaMessage> messages = kafka08Consumer.getMessages();
        logger.info("fetchCount: {} cost: {}, messages: {}", kafka08Consumer.fetchCount.get(), kafka08Consumer.getCost(), JSON.toJSONString(messages));
    }

}
