package com.abc.util.kafka;


import com.abc.vo.commonconfigvoproperty.KafkaProducerConfig;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import java.util.Properties;

public class KafkaProducer {

    private final Producer<String, String> producer;
    private final KafkaProducerConfig producerConfig;

    public KafkaProducer(KafkaProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
        Properties props = new Properties();
        //指定代理服务器的地址
        props.put("metadata.broker.list", producerConfig.getBroker());
        //配置value的序列化类
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        //配置key的序列化类
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        //request.required.acks
        //0, which means that the producer never waits for an acknowledgement from the broker (the same behavior as 0.7). This option provides the lowest latency but the weakest durability guarantees (some data will be lost when a server fails).
        //1, which means that the producer gets an acknowledgement after the leader replica has received the data. This option provides better durability as the client waits until the server acknowledges the request as successful (only messages that were written to the now-dead leader but not yet replicated will be lost).
        //-1, which means that the producer gets an acknowledgement after all in-sync replicas have received the data. This option provides the best durability, we guarantee that no messages will be lost as long as at least one in sync replica remains.
        props.put("request.required.acks", "-1");
        //创建producer 对象
        producer = new Producer<>(new ProducerConfig(props));
    }

    public void send() {
        producer.send(new KeyedMessage<>(producerConfig.getTopic(), producerConfig.getMessage()));
        producer.close();
    }

    public static void main(String[] args) {
        KafkaProducerConfig producerConfig = new KafkaProducerConfig();
        KafkaProducer producer = new KafkaProducer(producerConfig);
        producer.send();
    }

}
