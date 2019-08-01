package com.abc.util.kafka;

import com.abc.exception.MessageRuntimeException;
import com.abc.util.kafka.examples.DatasetFilterUtils;
import com.abc.vo.commonconfigvoproperty.KafkaTopicConfig;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import kafka.api.PartitionFetchInfo;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.MessageAndOffset;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class Kafka08OldConsumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka08OldConsumer.class);

    private static final ThreadLocal<SimpleConsumer> THEAD_CONSUMER = new ThreadLocal<>();

    private static final int WAIT_MAX_SECONDS = 30;
    private static final int MAX_MESSAGE_COUNT = 10;
    private static final int DEFAULT_KAFKA_TIMEOUT_VALUE = 30000;
    private static final int DEFAULT_KAFKA_BUFFER_SIZE = 1024 * 1024 * 10;
    private static final String DEFAULT_KAFKA_CLIENT_NAME = "__BIU__";
    private static final int DEFAULT_KAFKA_FETCH_REQUEST_CORRELATION_ID = 11;//-1
    private static final int DEFAULT_KAFKA_FETCH_REQUEST_MIN_BYTES = 1024;
    private static final int NUM_TRIES_FETCH_TOPIC = 3;
    private static final int NUM_TRIES_FETCH_OFFSET = 3;
    private static final long SEARCH_OFFSET_SIZE = 5000;
    private final Map<String, KafkaMessage> messages = new ConcurrentHashMap<>();
    private KafkaTopicConfig clusterConfig;
    private long start;
    private long cost;
    @Getter
    private AtomicLong fetchCount = new AtomicLong(0);
    @Getter
    private AtomicLong totalCount = new AtomicLong(0);

    public Kafka08OldConsumer(KafkaTopicConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
        start = System.currentTimeMillis();

        SimpleConsumer getTopicConsumer = createSimpleConsumer(clusterConfig.getBroker());
        THEAD_CONSUMER.set(getTopicConsumer);
        List<KafkaTopic> topics = getFilteredTopics(clusterConfig.getBroker(), Collections.emptyList(), Arrays.asList(Pattern.compile(clusterConfig.getTopic())));
        getTopicConsumer.close();
        THEAD_CONSUMER.remove();

        KafkaTopic kafkaTopic = topics.get(0);//如果为空, 则不存在该topic

        ExecutorService executor = Executors.newFixedThreadPool(kafkaTopic.getPartitions().size());
        for (int i = 0; i < kafkaTopic.getPartitions().size(); i++) {
            final KafkaPartition kafkaPartition = kafkaTopic.getPartitions().get(i);
            executor.submit(() -> {
                //查询offset
                SimpleConsumer consumer = createSimpleConsumer(kafkaPartition.getLeader().getHostAndPort());
                THEAD_CONSUMER.set(consumer);
                try {
                    long earliestOffset = getEarliestOffset(kafkaPartition);
                    long latestOffset = getLatestOffset(kafkaPartition);
                    totalCount.addAndGet(latestOffset - earliestOffset);
                    logger.info("fetch topic {} partition {} offset {}-{}", clusterConfig.getTopic(), kafkaPartition.getId(), earliestOffset, latestOffset);

                    KafkaOffsetPage kafkaOffsetPage = new KafkaOffsetPage(earliestOffset, latestOffset, SEARCH_OFFSET_SIZE);
                    int pageIndex = 1;
                    long[] offsetRange = kafkaOffsetPage.getOffsetRange(pageIndex);
                    while (offsetRange != KafkaOffsetPage.EMPTY) {
                        logger.info("fetch topic {} partition {} times {} offset {}-{}", clusterConfig.getTopic(), kafkaPartition.getId(), pageIndex, offsetRange[0], offsetRange[1]);
                        fetchNextMessageBuffer(kafkaPartition, offsetRange[0], offsetRange[1]);
                        pageIndex++;
                        //判断是否需要退出
                        int messageSize = messages.size();
                        if (messageSize >= MAX_MESSAGE_COUNT || (System.currentTimeMillis() - start) / 1000 > WAIT_MAX_SECONDS) {
                            logger.info("partition: {} fetch break. messageSize:{}", kafkaPartition.getId(), messageSize);
                            break;
                        }
                        offsetRange = kafkaOffsetPage.getOffsetRange(pageIndex);
                    }
                } catch (KafkaOffsetRetrievalFailureException e) {
                    //do nothing
                } finally {
                    consumer.close();
                }
            });
        }

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

    public void fetchNextMessageBuffer(KafkaPartition partition, long minOffset,
                                       long maxOffset) {
        if (minOffset > maxOffset) {
            return;
        }
        SimpleConsumer consumer = null;
        try {
            consumer = THEAD_CONSUMER.get();
            long nextOffset = minOffset;
            int fetchTimes = 1;
            while (true) {
                FetchRequest fetchRequest = createFetchRequest(partition, nextOffset);
                FetchResponse fetchResponse = getFetchResponseForFetchRequest(consumer, fetchRequest, partition);
                Iterator<MessageAndOffset> iteratorFromFetchResponse = getIteratorFromFetchResponse(fetchResponse, partition);
                logger.info("fetch topic {} partition {} fetchTimes: {} offset {}", partition.getTopicName(), partition.getId(), fetchTimes, nextOffset);
                MessageAndOffset last = null;
                while (iteratorFromFetchResponse != null && iteratorFromFetchResponse.hasNext()) {
                    last = iteratorFromFetchResponse.next();
                    ByteBuffer payload = last.message().payload();
                    byte[] bytes = new byte[payload.limit()];
                    payload.get(bytes);
                    String message = new String(bytes, "UTF-8");
                    fetchCount.incrementAndGet();
                    if (match(message)) {
                        messages.put(String.format("%s-%s", partition.getId(), last.offset()), new KafkaMessage(null, message, null));
                    }
                    if (last.nextOffset() >= maxOffset) {
                        break;
                    }
                }
                if (last == null) {
                    return;
                }
                if (last.nextOffset() >= maxOffset) {
                    break;
                }
                nextOffset = last.nextOffset();
                fetchTimes++;
            }
        } catch (Exception e) {
            logger.warn("Fetch message buffer for partition {} has failed. Will refresh topic metadata and retry. ",
                    partition, e);
            throw new MessageRuntimeException("获取Kafka数据异常", e);
        }
    }

    private FetchRequest createFetchRequest(KafkaPartition partition, long nextOffset) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(partition.getTopicName(), partition.getId());
        PartitionFetchInfo partitionFetchInfo = new PartitionFetchInfo(nextOffset, DEFAULT_KAFKA_BUFFER_SIZE);
        Map<TopicAndPartition, PartitionFetchInfo> fetchInfo = Collections.singletonMap(topicAndPartition, partitionFetchInfo);
        return new FetchRequest(DEFAULT_KAFKA_FETCH_REQUEST_CORRELATION_ID, DEFAULT_KAFKA_CLIENT_NAME,
                DEFAULT_KAFKA_TIMEOUT_VALUE, DEFAULT_KAFKA_FETCH_REQUEST_MIN_BYTES, fetchInfo);
    }

    private Iterator<MessageAndOffset> getIteratorFromFetchResponse(FetchResponse fetchResponse,
                                                                    KafkaPartition partition) {
        try {
            ByteBufferMessageSet messageBuffer = fetchResponse.messageSet(partition.getTopicName(), partition.getId());
            return messageBuffer.iterator();
        } catch (Exception e) {
            logger.warn(String.format("Failed to retrieve next message buffer for partition %s: %s."
                    + "The remainder of this partition will be skipped.", partition, e));
            return null;
        }
    }

    private FetchResponse getFetchResponseForFetchRequest(SimpleConsumer consumer,
                                                          FetchRequest fetchRequest,
                                                          KafkaPartition partition) {
        FetchResponse fetchResponse = consumer.fetch(fetchRequest);
        if (fetchResponse.hasError()) {
            consumer.close();
            throw new RuntimeException(
                    String.format("error code %d", fetchResponse.errorCode(partition.getTopicName(), partition.getId())));
        }
        return fetchResponse;
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

    public long getEarliestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException {
        Map<TopicAndPartition, PartitionOffsetRequestInfo> offsetRequestInfo =
                Collections.singletonMap(new TopicAndPartition(partition.getTopicName(), partition.getId()),
                        new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.EarliestTime(), 1));
        return getOffset(partition, offsetRequestInfo);
    }

    public long getLatestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException {
        Map<TopicAndPartition, PartitionOffsetRequestInfo> offsetRequestInfo =
                Collections.singletonMap(new TopicAndPartition(partition.getTopicName(), partition.getId()),
                        new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.LatestTime(), 1));
        return getOffset(partition, offsetRequestInfo);
    }

    private long getOffset(KafkaPartition partition,
                           Map<TopicAndPartition, PartitionOffsetRequestInfo> offsetRequestInfo)
            throws KafkaOffsetRetrievalFailureException {
        SimpleConsumer consumer = THEAD_CONSUMER.get();
        for (int i = 0; i < NUM_TRIES_FETCH_OFFSET; i++) {
            try {
                OffsetResponse offsetResponse = consumer.getOffsetsBefore(new OffsetRequest(offsetRequestInfo,
                        kafka.api.OffsetRequest.CurrentVersion(), DEFAULT_KAFKA_CLIENT_NAME));
                if (offsetResponse.hasError()) {
                    throw new RuntimeException(
                            "offsetReponse has error: " + offsetResponse.errorCode(partition.getTopicName(), partition.getId()));
                }
                return offsetResponse.offsets(partition.getTopicName(), partition.getId())[0];
            } catch (Exception e) {
                logger.warn(
                        String.format("Fetching offset for partition %s has failed %d time(s). Reason: %s", partition, i + 1, e));
                if (i < NUM_TRIES_FETCH_OFFSET - 1) {
                    try {
                        Thread.sleep((long) ((i + Math.random()) * 1000));
                    } catch (InterruptedException e2) {
                        logger.error("Caught interrupted exception between retries of getting latest offsets. " + e2);
                    }
                }
            }
        }
        throw new KafkaOffsetRetrievalFailureException(
                String.format("Fetching offset for partition %s has failed.", partition));
    }

    public List<KafkaTopic> getFilteredTopics(String broker, List<Pattern> blacklist, List<Pattern> whitelist) {
        List<TopicMetadata> topicMetadataList = getFilteredMetadataList(broker, blacklist, whitelist);

        List<KafkaTopic> filteredTopics = Lists.newArrayList();
        for (TopicMetadata topicMetadata : topicMetadataList) {
            List<KafkaPartition> partitions = getPartitionsForTopic(topicMetadata);
            filteredTopics.add(new KafkaTopic(topicMetadata.topic(), partitions));
        }
        return filteredTopics;
    }

    private List<KafkaPartition> getPartitionsForTopic(TopicMetadata topicMetadata) {
        List<KafkaPartition> partitions = Lists.newArrayList();

        for (PartitionMetadata partitionMetadata : topicMetadata.partitionsMetadata()) {
            if (partitionMetadata.leader() == null) {
                //单机
                partitions.add(new KafkaPartition.Builder().withId(partitionMetadata.partitionId())
                        .withTopicName(topicMetadata.topic())
                        .build());
            } else {
                partitions.add(new KafkaPartition.Builder().withId(partitionMetadata.partitionId())
                        .withTopicName(topicMetadata.topic()).withLeaderId(partitionMetadata.leader().id())
                        .withLeaderHostAndPort(partitionMetadata.leader().host(), partitionMetadata.leader().port()).build());
            }
        }
        return partitions;
    }

    private List<TopicMetadata> getFilteredMetadataList(String broker, List<Pattern> blacklist, List<Pattern> whitelist) {
        List<TopicMetadata> filteredTopicMetadataList = Lists.newArrayList();

        //Try all brokers one by one, until successfully retrieved topic metadata (topicMetadataList is non-null)
        filteredTopicMetadataList = fetchTopicMetadataFromBroker(broker, blacklist, whitelist);
        if (filteredTopicMetadataList != null) {
            return filteredTopicMetadataList;
        }

        throw new RuntimeException(
                "Fetching topic metadata from all brokers failed. See log warning for more information.");
    }

    private List<TopicMetadata> fetchTopicMetadataFromBroker(String broker, List<Pattern> blacklist,
                                                             List<Pattern> whitelist) {

        List<TopicMetadata> topicMetadataList = fetchTopicMetadataFromBroker(broker);
        if (topicMetadataList == null) {
            return null;
        }

        List<TopicMetadata> filteredTopicMetadataList = Lists.newArrayList();
        for (TopicMetadata topicMetadata : topicMetadataList) {
            if (DatasetFilterUtils.survived(topicMetadata.topic(), blacklist, whitelist)) {
                filteredTopicMetadataList.add(topicMetadata);
            }
        }
        return filteredTopicMetadataList;
    }

    private List<TopicMetadata> fetchTopicMetadataFromBroker(String broker, String... selectedTopics) {
        logger.info(String.format("Fetching topic metadata from broker %s", broker));
        SimpleConsumer consumer = null;
        consumer = THEAD_CONSUMER.get();
        for (int i = 0; i < NUM_TRIES_FETCH_TOPIC; i++) {
            try {
                return consumer.send(new TopicMetadataRequest(Arrays.asList(selectedTopics))).topicsMetadata();
            } catch (Exception e) {
                logger.warn(String.format("Fetching topic metadata from broker %s has failed %d times.", broker, i + 1), e);
                try {
                    Thread.sleep((long) ((i + Math.random()) * 1000));
                } catch (InterruptedException e2) {
                    logger.warn("Caught InterruptedException: " + e2);
                }
            }
        }
        return null;
    }

    private SimpleConsumer createSimpleConsumer(String broker) {
        List<String> hostPort = Splitter.on(':').trimResults().omitEmptyStrings().splitToList(broker);
        return createSimpleConsumer(hostPort.get(0), Integer.parseInt(hostPort.get(1)));
    }

    private SimpleConsumer createSimpleConsumer(String host, int port) {
        return new SimpleConsumer(host, port, DEFAULT_KAFKA_TIMEOUT_VALUE, DEFAULT_KAFKA_BUFFER_SIZE,
                DEFAULT_KAFKA_CLIENT_NAME);
    }

    private SimpleConsumer createSimpleConsumer(HostAndPort hostAndPort) {
        return new SimpleConsumer(hostAndPort.getHost(), hostAndPort.getPort(), DEFAULT_KAFKA_TIMEOUT_VALUE, DEFAULT_KAFKA_BUFFER_SIZE,
                DEFAULT_KAFKA_CLIENT_NAME);
    }

    public Map<String, KafkaMessage> getMessages() {
        return this.messages;
    }

    public long getCost() {
        return this.cost;
    }

    public static void main(String[] args) {
        KafkaTopicConfig kafkaTopicConfig = new KafkaTopicConfig();
        kafkaTopicConfig.setBroker("10.202.24.5:9096");
        kafkaTopicConfig.setClusterName("bus");
        kafkaTopicConfig.setTopic("SHIVA_OMS_UNCALL_ACC_TO_SGS");
        kafkaTopicConfig.setKeyword("03201072916045342308052719");
        Kafka08OldConsumer kafka08Consumer = new Kafka08OldConsumer(kafkaTopicConfig);
        Map<String, KafkaMessage> messages = kafka08Consumer.getMessages();
        logger.info("fetchCount: {} totalCount: {} cost: {}, message size: {} messages: {}", kafka08Consumer.fetchCount.get(), kafka08Consumer.getTotalCount().get(), kafka08Consumer.getCost(), messages.size(), JSON.toJSONString(messages));
    }

}
