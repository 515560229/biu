package com.abc.util.kafka;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;
import kafka.utils.VerifiableProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class KafkaJavaOldHighConsumer {
    private final ConsumerConnector consumer;

    public KafkaJavaOldHighConsumer() {
        Properties originalProps = new Properties();
        //zookeeper 配置，通过zk 可以负载均衡的获取broker
        //10.203.26.176:2181/kafka/inc-sgs    10.202.24.5:2181/kafka/bus  "10.202.34.28:2182/kafka1.1.0/default"
        originalProps.put("zookeeper.connect", KafkaConstants.ZK_8);
        originalProps.put("auto.commit.enable", "false");
        originalProps.put("fetch.message.max.bytes", String.valueOf(1024 * 1024 * 10));
        //group 代表一个消费组
        originalProps.put("group.id", "_myGroup10012_");
        //zk连接超时时间
        originalProps.put("zookeeper.session.timeout.ms", "10000");
        //zk同步时间
        originalProps.put("zookeeper.sync.time.ms", "200");
        //自动提交间隔时间
        originalProps.put("auto.commit.interval.ms", "1000");
        //消息日志自动偏移量,防止宕机后数据无法读取
        originalProps.put("auto.offset.reset", "smallest");
        originalProps.put("connections.max.idle.ms", "600000");
        //序列化类
//        originalProps.put("serializer.class", "kafka.serializer.StringEncoder");

//        originalProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
//        originalProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
//        originalProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        //构建consumer connection 对象
        consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(originalProps));
    }

    public void consume() throws InterruptedException {
        // "CIAM_PAY_RET"  a-b.c-ef.ax-d.xxxx-test18.sgs.env-1/ 这个可以消费
        String topic = KafkaConstants.TOPIC_8;
        //指定需要订阅的topic
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, new Integer(32));//线程数
        //指定key的编码格式
        Decoder<String> keyDecoder = new kafka.serializer.StringDecoder(new VerifiableProperties());
        //指定value的编码格式
        Decoder<String> valueDecoder = new kafka.serializer.StringDecoder(new VerifiableProperties());
        //获取topic 和 接受到的stream 集合
        Map<String, List<KafkaStream<String, String>>> map = consumer.createMessageStreams(topicCountMap, keyDecoder, valueDecoder);

        //根据指定的topic 获取 stream 集合
        List<KafkaStream<String, String>> kafkaStreams = map.get(topic);
        ExecutorService executor = Executors.newFixedThreadPool(32);

        AtomicInteger count = new AtomicInteger(0);

        //因为是多个 message组成 message set ， 所以要对stream 进行拆解遍历
        for (final KafkaStream<String, String> kafkaStream : kafkaStreams) {
            //拆解每个的 stream
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println(1);

                    ConsumerIterator<String, String> iterator = kafkaStream.iterator();
                    while (iterator.hasNext()) {
                        System.out.println(2);
                        //messageAndMetadata 包括了 message ， topic ， partition等metadata信息
                        MessageAndMetadata<String, String> messageAndMetadata = iterator.next();
                        System.out.println("message : " + messageAndMetadata.message());
                        System.out.println("partition :  " + messageAndMetadata.partition() + " offset: " + messageAndMetadata.offset() + " count: " + count.incrementAndGet());
                    }
                }
            });
        }
        int seconds = 1;
        while (true) {
            if (count.intValue() >= 10 || seconds > 10) {
                break;
            }
            seconds++;
            Thread.sleep(1000 * 1);
        }
        consumer.shutdown();
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000 * 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("finish and close");
    }

    public static void main(String[] args) throws InterruptedException {
        new KafkaJavaOldHighConsumer().consume();
    }

}
