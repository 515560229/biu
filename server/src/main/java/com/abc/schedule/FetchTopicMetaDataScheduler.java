package com.abc.schedule;

import com.abc.constant.ConfigType;
import com.abc.entity.CommonConfig;
import com.abc.service.CommonConfigService;
import com.abc.util.kafka.KafkaTopic;
import com.abc.util.kafka.KafkaTopicFetcher;
import com.abc.vo.commonconfigvoproperty.KafkaClusterConfig;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Configuration
public class FetchTopicMetaDataScheduler {
    private static final Logger logger = LoggerFactory.getLogger(FetchTopicMetaDataScheduler.class);

    @Autowired
    private KafkaTopicFetcher kafkaTopicFetcher;

    @Setter
    private List<KafkaClusterConfig> kafkaClusterConfigList;

    @Autowired
    private CommonConfigService commonConfigService;

    @PostConstruct
    public void init() throws IOException {
        Resource resource = new ClassPathResource("kafkaCluster.json");
        kafkaClusterConfigList = JSON.parseArray(IOUtils.toString(resource.getInputStream(), "UTF-8"), KafkaClusterConfig.class);
        if (logger.isInfoEnabled()) {
            logger.info("kafka cluster info: {}", JSON.toJSONString(kafkaClusterConfigList));
        }
    }

    private String toName(String cluster, String topicName) {
        return String.format("%s.%s", cluster, topicName);
    }

    @Scheduled(cron = "0 0 9/4 ? * *")
//    @Scheduled(cron = "0/30 * * * * ?")
    public void fetchTopic() {
        logger.info("fetch topic job start.");
        if (kafkaClusterConfigList == null) {
            logger.info("kafka cluster is null");
            return;
        }
        for (KafkaClusterConfig kafkaClusterConfig : kafkaClusterConfigList) {
            logger.info("fetch topic meta data start. cluster: {}", kafkaClusterConfig.getClusterName());
            try {
                fetchTopic(kafkaClusterConfig);
            } catch (Exception ex) {
                logger.info("fetch topic meta data error. cluster: {}", kafkaClusterConfig.getClusterName(), ex);
            }
            logger.info("fetch topic meta data end. cluster: {}", kafkaClusterConfig.getClusterName());
        }

    }

    private void fetchTopic(KafkaClusterConfig kafkaClusterConfig) {
        List<KafkaTopic> kafkaTopics = kafkaTopicFetcher.fetch(kafkaClusterConfig);
        if (CollectionUtils.isNotEmpty(kafkaTopics)) {
            for (KafkaTopic kafkaTopic : kafkaTopics) {
                String configName = toName(kafkaClusterConfig.getClusterName(), kafkaTopic.getName());
                CommonConfig commonConfig = commonConfigService.selectOne(new EntityWrapper<CommonConfig>()
                        .eq("name", configName)
                        .eq("type", ConfigType.KAFKA_TOPIC_INFO.getValue()));
                if (commonConfig == null) {
                    commonConfig = new CommonConfig();
                    commonConfig.setCreated(new Date());
                    commonConfig.setCreator("system");
                    commonConfig.setName(configName);
                    commonConfig.setType(ConfigType.KAFKA_TOPIC_INFO.getValue());
                }
                commonConfig.setModifier("system");
                commonConfig.setUpdated(new Date());
                kafkaTopic.setVersion(kafkaClusterConfig.getVersion());
                commonConfig.setValue(JSON.toJSONString(kafkaTopic));
                try {
                    commonConfigService.insertOrUpdate(commonConfig);
                } catch (Exception ex) {
                    logger.info("fetch topic fail when saveOrUpdate. entity: {}", JSON.toJSONStringWithDateFormat(commonConfig, "yyyy-MM-dd HH:mm:ss"), ex);
                }
            }
        }
    }

}
