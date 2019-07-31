package com.abc.util.kafka;

import com.abc.util.kafka.examples.DatasetFilterUtils;
import com.abc.util.kafka.examples.KafkaOffsetRetrievalFailureException;
import com.abc.vo.commonconfigvoproperty.KafkaClusterConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import kafka.api.PartitionFetchInfo;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.MessageAndOffset;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

// 老版 低阶 API 无法消费高版本的broker 1.1.0的
public class KafkaTopicFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaTopicFetcher.class);

    @Getter
    private final KafkaAPI kafkaAPI = new KafkaOldAPI();

    @PostConstruct
    public List<KafkaTopic> fetch(KafkaClusterConfig clusterConfig) {
        List<Pattern> blackPatterns = new ArrayList<>();
        blackPatterns.add(Pattern.compile("__consumer_offsets"));

        return kafkaAPI.getFilteredTopics(clusterConfig.getBroker(), blackPatterns, Collections.emptyList());
    }

    public static abstract class KafkaAPI implements Closeable {
        protected abstract List<KafkaTopic> getFilteredTopics(String cluster, List<Pattern> blacklist, List<Pattern> whitelist);

        protected abstract long getEarliestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException;

        protected abstract long getLatestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException;

        protected abstract Iterator<MessageAndOffset> fetchNextMessageBuffer(String cluster, KafkaPartition partition, long nextOffset,
                                                                             long maxOffset);
    }

    /**
     * Wrapper for the old low-level Scala-based Kafka API.
     */
    public static class KafkaOldAPI extends KafkaAPI {
        private static final int DEFAULT_KAFKA_TIMEOUT_VALUE = 30000;
        private static final int DEFAULT_KAFKA_BUFFER_SIZE = 1024 * 1024 * 1024;
        private static final String DEFAULT_KAFKA_CLIENT_NAME = "__biu-client__";
        private static final int DEFAULT_KAFKA_FETCH_REQUEST_CORRELATION_ID = 11;//-1
        private static final int DEFAULT_KAFKA_FETCH_REQUEST_MIN_BYTES = 1024;
        private static final int NUM_TRIES_FETCH_TOPIC = 3;
        private static final int NUM_TRIES_FETCH_OFFSET = 3;

        private final ConcurrentMap<String, SimpleConsumer> activeConsumers = Maps.newConcurrentMap();

        @Override
        public List<KafkaTopic> getFilteredTopics(String cluster, List<Pattern> blacklist, List<Pattern> whitelist) {
            List<TopicMetadata> topicMetadataList = getFilteredMetadataList(cluster, blacklist, whitelist);

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

        private List<TopicMetadata> getFilteredMetadataList(String cluster, List<Pattern> blacklist, List<Pattern> whitelist) {
            List<TopicMetadata> filteredTopicMetadataList = Lists.newArrayList();

            //Try all brokers one by one, until successfully retrieved topic metadata (topicMetadataList is non-null)
            filteredTopicMetadataList = fetchTopicMetadataFromBroker(cluster, blacklist, whitelist);
            if (filteredTopicMetadataList != null) {
                return filteredTopicMetadataList;
            }

            throw new RuntimeException(
                    "Fetching topic metadata from all brokers failed. See log warning for more information.");
        }

        private List<TopicMetadata> fetchTopicMetadataFromBroker(String cluster, List<Pattern> blacklist,
                                                                 List<Pattern> whitelist) {

            List<TopicMetadata> topicMetadataList = fetchTopicMetadataFromBroker(cluster);
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
            LOG.info(String.format("Fetching topic metadata from cluster %s", broker));
            SimpleConsumer consumer = null;
            try {
                consumer = createSimpleConsumer(broker);
                for (int i = 0; i < NUM_TRIES_FETCH_TOPIC; i++) {
                    try {
                        return consumer.send(new TopicMetadataRequest(Arrays.asList(selectedTopics))).topicsMetadata();
                    } catch (Exception e) {
                        LOG.warn(String.format("Fetching topic metadata from cluster %s has failed %d times.", broker, i + 1), e);
                        try {
                            Thread.sleep((long) ((i + Math.random()) * 1000));
                        } catch (InterruptedException e2) {
                            LOG.warn("Caught InterruptedException: " + e2);
                        }
                    }
                }
            } finally {
                if (consumer != null) {
                    consumer.close();
                }
            }
            return null;
        }

        @Override
        protected long getEarliestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException {
            Map<TopicAndPartition, PartitionOffsetRequestInfo> offsetRequestInfo =
                    Collections.singletonMap(new TopicAndPartition(partition.getTopicName(), partition.getId()),
                            new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.EarliestTime(), 1));
            return getOffset(partition, offsetRequestInfo);
        }

        @Override
        protected long getLatestOffset(KafkaPartition partition) throws KafkaOffsetRetrievalFailureException {
            Map<TopicAndPartition, PartitionOffsetRequestInfo> offsetRequestInfo =
                    Collections.singletonMap(new TopicAndPartition(partition.getTopicName(), partition.getId()),
                            new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.LatestTime(), 1));
            return getOffset(partition, offsetRequestInfo);
        }

        private long getOffset(KafkaPartition partition,
                               Map<TopicAndPartition, PartitionOffsetRequestInfo> offsetRequestInfo)
                throws KafkaOffsetRetrievalFailureException {
            SimpleConsumer consumer = createSimpleConsumer(partition.getLeader().getHostAndPort());
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
                    LOG.warn(
                            String.format("Fetching offset for partition %s has failed %d time(s). Reason: %s", partition, i + 1, e));
                    if (i < NUM_TRIES_FETCH_OFFSET - 1) {
                        try {
                            Thread.sleep((long) ((i + Math.random()) * 1000));
                        } catch (InterruptedException e2) {
                            LOG.error("Caught interrupted exception between retries of getting latest offsets. " + e2);
                        }
                    }
                } finally {
                    consumer.close();
                }
            }
            throw new KafkaOffsetRetrievalFailureException(
                    String.format("Fetching offset for partition %s has failed.", partition));
        }

        @Override
        protected Iterator<MessageAndOffset> fetchNextMessageBuffer(String cluster,
                                                                    KafkaPartition partition, long nextOffset,
                                                                    long maxOffset) {
            if (nextOffset > maxOffset) {
                return null;
            }

            FetchRequest fetchRequest = createFetchRequest(partition, nextOffset);

            try {
                FetchResponse fetchResponse = getFetchResponseForFetchRequest(fetchRequest, partition);
                return getIteratorFromFetchResponse(fetchResponse, partition);
            } catch (Exception e) {
                LOG.warn("Fetch message buffer for partition {} has failed. Will refresh topic metadata and retry. ",
                                partition, e);
                return refreshTopicMetadataAndRetryFetch(cluster, partition, fetchRequest);
            }
        }

        private synchronized FetchResponse getFetchResponseForFetchRequest(FetchRequest fetchRequest,
                                                                           KafkaPartition partition) {
            SimpleConsumer consumer = createSimpleConsumer(partition.getLeader().getHostAndPort());

            FetchResponse fetchResponse = consumer.fetch(fetchRequest);
            if (fetchResponse.hasError()) {
                consumer.close();
                throw new RuntimeException(
                        String.format("error code %d", fetchResponse.errorCode(partition.getTopicName(), partition.getId())));
            }
            consumer.close();
            return fetchResponse;
        }

        private Iterator<MessageAndOffset> getIteratorFromFetchResponse(FetchResponse fetchResponse,
                                                                        KafkaPartition partition) {
            try {
                ByteBufferMessageSet messageBuffer = fetchResponse.messageSet(partition.getTopicName(), partition.getId());
                return messageBuffer.iterator();
            } catch (Exception e) {
                LOG.warn(String.format("Failed to retrieve next message buffer for partition %s: %s."
                        + "The remainder of this partition will be skipped.", partition, e));
                return null;
            }
        }

        private Iterator<MessageAndOffset> refreshTopicMetadataAndRetryFetch(String cluster,
                                                                             KafkaPartition partition,
                                                                             FetchRequest fetchRequest) {
            try {
                refreshTopicMetadata(cluster, partition);
                FetchResponse fetchResponse = getFetchResponseForFetchRequest(fetchRequest, partition);
                return getIteratorFromFetchResponse(fetchResponse, partition);
            } catch (Exception e) {
                LOG.warn(String.format("Fetch message buffer for partition %s has failed: %s. This partition will be skipped.",
                        partition, e));
                return null;
            }
        }

        private void refreshTopicMetadata(String cluster, KafkaPartition partition) {
            List<TopicMetadata> topicMetadataList = fetchTopicMetadataFromBroker(cluster, partition.getTopicName());
            if (topicMetadataList != null && !topicMetadataList.isEmpty()) {
                TopicMetadata topicMetadata = topicMetadataList.get(0);
                for (PartitionMetadata partitionMetadata : topicMetadata.partitionsMetadata()) {
                    if (partitionMetadata.partitionId() == partition.getId()) {
                        partition.setLeader(partitionMetadata.leader().id(), partitionMetadata.leader().host(),
                                partitionMetadata.leader().port());
                        break;
                    }
                }
            }
        }

        private FetchRequest createFetchRequest(KafkaPartition partition, long nextOffset) {
            TopicAndPartition topicAndPartition = new TopicAndPartition(partition.getTopicName(), partition.getId());
            PartitionFetchInfo partitionFetchInfo = new PartitionFetchInfo(nextOffset, DEFAULT_KAFKA_BUFFER_SIZE);
            Map<TopicAndPartition, PartitionFetchInfo> fetchInfo =
                    Collections.singletonMap(topicAndPartition, partitionFetchInfo);
            return new FetchRequest(DEFAULT_KAFKA_FETCH_REQUEST_CORRELATION_ID, DEFAULT_KAFKA_CLIENT_NAME,
                    DEFAULT_KAFKA_TIMEOUT_VALUE, DEFAULT_KAFKA_FETCH_REQUEST_MIN_BYTES, fetchInfo);
        }

        @Override
        public void close() throws IOException {
            int numOfConsumersNotClosed = 0;

            for (SimpleConsumer consumer : this.activeConsumers.values()) {
                if (consumer != null) {
                    try {
                        consumer.close();
                    } catch (Exception e) {
                        LOG.warn(String.format("Failed to close Kafka Consumer %s:%d", consumer.host(), consumer.port()));
                        numOfConsumersNotClosed++;
                    }
                }
            }
            this.activeConsumers.clear();
            if (numOfConsumersNotClosed > 0) {
                throw new IOException(numOfConsumersNotClosed + " consumer(s) failed to close.");
            }
        }

        private SimpleConsumer createSimpleConsumer(String broker) {
            List<String> hostPort = Splitter.on(':').trimResults().omitEmptyStrings().splitToList(broker);
            return createSimpleConsumer(hostPort.get(0), Integer.parseInt(hostPort.get(1)));
        }

        private SimpleConsumer createSimpleConsumer(HostAndPort hostAndPort) {
            return new SimpleConsumer(hostAndPort.getHost(), hostAndPort.getPort(), DEFAULT_KAFKA_TIMEOUT_VALUE, DEFAULT_KAFKA_BUFFER_SIZE,
                    DEFAULT_KAFKA_CLIENT_NAME);
        }

        private SimpleConsumer createSimpleConsumer(String host, int port) {
            return new SimpleConsumer(host, port, DEFAULT_KAFKA_TIMEOUT_VALUE, DEFAULT_KAFKA_BUFFER_SIZE,
                    DEFAULT_KAFKA_CLIENT_NAME);
        }
    }

    public static void main(String[] args) {
        KafkaTopicFetcher fetcher = new KafkaTopicFetcher();
        KafkaClusterConfig clusterConfig = new KafkaClusterConfig();
        clusterConfig.setBroker("localhost:19092");
        List<KafkaTopic> kafkaTopics = fetcher.fetch(clusterConfig);
        System.out.println(JSON.toJSONString(kafkaTopics, SerializerFeature.PrettyFormat));
    }

}
