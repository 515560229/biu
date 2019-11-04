package com.abc.util.kafka;

import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class KafkaConsumer {
    private static final int WAIT_MAX_SECONDS = 30;
    protected static final int MAX_MESSAGE_COUNT = 50;
    protected static final String GROUP_ID = "__BIU__";

    protected KafkaConsumerConfig kafkaConsumerConfig;
    @Setter
    protected int waitMaxSeconds;

    private ThreadLocal<Long> fetchCount = ThreadLocal.withInitial(() -> 0L);//kafka 推的形式, fetch的次数
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

    protected long fetchCountIncrementAndGet() {
        fetchCount.set(fetchCount.get() + 1);
        return getFetchCount();
    }

    public long getFetchCount() {
        return fetchCount.get();
    }

    protected boolean match(String message) {
        if (StringUtils.isBlank(kafkaConsumerConfig.getKeyword())) {
            return true;
        }
        String[] keywords = kafkaConsumerConfig.getKeyword().split(" ");
        int keywordsLength = keywords.length;
        int matchKeywordLength = 0;
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                matchKeywordLength++;
            }
        }
        if (keywordsLength == matchKeywordLength) {
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

    protected String randomGroup() {
        return UUID.randomUUID().toString();
    }

    public int getWaitMaxSeconds() {
        if (waitMaxSeconds <= 0) {
            return WAIT_MAX_SECONDS;
        }
        return waitMaxSeconds;
    }

    public abstract void consume();

}
