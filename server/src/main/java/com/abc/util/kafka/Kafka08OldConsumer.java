package com.abc.util.kafka;

import com.abc.exception.MessageRuntimeException;
import com.abc.util.kafka.examples.DatasetFilterUtils;
import com.abc.vo.commonconfigvoproperty.KafkaConsumerConfig;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import kafka.api.PartitionFetchInfo;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.FetchRequest;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.MessageAndOffset;

public class Kafka08OldConsumer extends KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(Kafka08OldConsumer.class);

    private static final ThreadLocal<SimpleConsumer> THEAD_CONSUMER = new ThreadLocal<>();

    private static final int DEFAULT_KAFKA_TIMEOUT_VALUE = 1000 * 5;
    private static final int DEFAULT_KAFKA_BUFFER_SIZE = 8 * 1024;
    private static final String DEFAULT_KAFKA_CLIENT_NAME = GROUP_ID;
    private static final int DEFAULT_KAFKA_FETCH_REQUEST_CORRELATION_ID = 11;//-1
    private static final int DEFAULT_KAFKA_FETCH_REQUEST_MIN_BYTES = 8;
    private static final int NUM_TRIES_FETCH_TOPIC = 3;
    private static final int NUM_TRIES_FETCH_OFFSET = 3;
    private static final long SEARCH_OFFSET_SIZE = 5000;

    public Kafka08OldConsumer(KafkaConsumerConfig kafkaConsumerConfig) {
        super(kafkaConsumerConfig);
        start = System.currentTimeMillis();
    }

    @Override
    public void consume() {
        SimpleConsumer getTopicConsumer = createSimpleConsumer(kafkaConsumerConfig.getBroker());
        THEAD_CONSUMER.set(getTopicConsumer);
        List<KafkaTopic> topics = getFilteredTopics(kafkaConsumerConfig.getBroker(), Collections.emptyList(), Arrays.asList(Pattern.compile(kafkaConsumerConfig.getTopic())));
        getTopicConsumer.close();
        THEAD_CONSUMER.remove();

        KafkaTopic kafkaTopic = topics.get(0);//如果为空, 则不存在该topic

        ExecutorService executor = Executors.newFixedThreadPool(kafkaTopic.getPartitions().size());
        for (int i = 0; i < kafkaTopic.getPartitions().size(); i++) {
            final KafkaPartition kafkaPartition = kafkaTopic.getPartitions().get(i);
            executor.submit(() -> {
                //查询offset
                SimpleConsumer consumer = createSimpleConsumer(kafkaPartition.getLeader().geetHostAndPort());
                THEAD_CONSUMER.set(consumer);
                try {
                    long earliestOffset = getEarliestOffset(kafkaPartition);
                    long latestOffset = getLatestOffset(kafkaPartition);
                    totalCount.addAndGet(latestOffset - earliestOffset);
                    logger.info("fetch topic {} partition {} offset {}-{}.total:{}", kafkaConsumerConfig.getTopic(), kafkaPartition.getId(), earliestOffset, latestOffset, latestOffset - earliestOffset);

                    KafkaOffsetPage kafkaOffsetPage = new KafkaOffsetPage(earliestOffset, latestOffset, SEARCH_OFFSET_SIZE);

                    int pageIndex = 1;
                    long[] offsetRange = kafkaOffsetPage.getOffsetRange(pageIndex);
                    while (offsetRange != KafkaOffsetPage.EMPTY) {
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
                } catch (Exception io){
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
                logger.info("wait executor terminated for topic {} {}", kafkaConsumerConfig.getClusterName(), kafkaConsumerConfig.getTopic());
            } catch (InterruptedException e) {
                logger.error("中断失败", e);
            }
        }
        cost = System.currentTimeMillis() - start;
        logger.info("finish and close for topic {} {}", kafkaConsumerConfig.getClusterName(), kafkaConsumerConfig.getTopic());
    }

    public void fetchNextMessageBuffer(KafkaPartition partition, long minOffset,
                                       long maxOffset) {
        if (minOffset > maxOffset) {
            return;
        }
        SimpleConsumer consumer = THEAD_CONSUMER.get();
        try {
            long nextOffset = minOffset;
            while (true) {
                FetchRequest fetchRequest = createFetchRequest(partition, nextOffset);
                long fetchCountValue = fetchCountIncrementAndGet();
                logger.info("fetch topic {} partition {} fetchTimes: {} offsetStart {}", partition.getTopicName(), partition.getId(), fetchCountValue, nextOffset);
                FetchResponse fetchResponse = getFetchResponseForFetchRequest(consumer, fetchRequest, partition);
                Iterator<MessageAndOffset> iteratorFromFetchResponse = getIteratorFromFetchResponse(fetchResponse, partition);
                MessageAndOffset last = null;
                while (iteratorFromFetchResponse != null && iteratorFromFetchResponse.hasNext()) {
                    last = iteratorFromFetchResponse.next();
                    logger.debug("fetch topic {} partition {} fetchTimes: {} offsetStart {} current {}", partition.getTopicName(), partition.getId(), fetchCountValue, nextOffset, last.offset());
                    ByteBuffer payload = last.message().payload();
                    byte[] bytes = new byte[payload.limit()];
                    payload.get(bytes);
                    String message = new String(bytes, "UTF-8");
                    if (match(message)) {
                        messages.put(getMessageKey(partition.getId(), last.offset()), new KafkaMessage(partition.getId(), last.offset(), null, message, null));
                    }
                    if (last.nextOffset() >= maxOffset) {
                        break;
                    }
                }
                if (last != null) {
                    logger.info("fetch topic {} partition {} fetchTimes: {} offsetEnd {}", partition.getTopicName(), partition.getId(), fetchCountValue, last.offset());
                } else {
                    logger.info("fetch topic {} partition {} fetchTimes: {} no data", partition.getTopicName(), partition.getId(), fetchCountValue);
                    return;
                }
                if (last.nextOffset() >= maxOffset) {
                    break;
                }
                nextOffset = last.nextOffset();
            }
        } catch (Exception e) {
            logger.warn("Fetch message buffer for partition {} has failed. Will refresh topic metadata and retry. ",
                    partition, e);
            throw new MessageRuntimeException("获取Kafka数据异常", e);
        }
    }

    private FetchRequest createFetchRequest(KafkaPartition partition, long nextOffset) {
        // 关于fetchSize  https://blog.csdn.net/wangqyoho/article/details/60571237
        // 关于minBytes https://blog.csdn.net/stark_summer/article/details/50203133
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

    private long getEarliestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException {
        Map<TopicAndPartition, PartitionOffsetRequestInfo> offsetRequestInfo =
                Collections.singletonMap(new TopicAndPartition(partition.getTopicName(), partition.getId()),
                        new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.EarliestTime(), 1));
        return getOffset(partition, offsetRequestInfo);
    }

    private long getLatestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException {
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

    private List<KafkaTopic> getFilteredTopics(String broker, List<Pattern> blacklist, List<Pattern> whitelist) {
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
}
