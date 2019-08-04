package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class KafkaConsumer {
    protected static final int WAIT_MAX_SECONDS = 30;
    protected static final int MAX_MESSAGE_COUNT = 10;
    protected static final String GROUP_ID = "__BIU__";

    protected KafkaConsumerConfig kafkaConsumerConfig;
    @Getter
    protected AtomicLong fetchCount = new AtomicLong(0);//kafka 推的形式, fetch的次数
    @Getter
    protected AtomicLong totalCount = new AtomicLong(0);//一共读取了多少条消息
    @Getter
    protected final Map<String, KafkaMessage> messages = new ConcurrentHashMap<>();
    protected long start;
    @Getter
    protected long cost;

    public KafkaConsumer(KafkaConsumerConfig kafkaConsumerConfig) {
        start = System.currentTimeMillis();
        this.kafkaConsumerConfig = kafkaConsumerConfig;
    }

    protected boolean match(String message) {
        if (StringUtils.isBlank(kafkaConsumerConfig.getKeyword())) {
            return true;
        }
        if (message.contains(kafkaConsumerConfig.getKeyword())) {
            return true;
        }
        return false;
    }

    protected String getMessageKey(int partition, long offset) {
        return String.format("%s-%s", partition, offset);
    }

    protected String toString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return e.getMessage();
        }
    }

    public abstract void consume();
}
