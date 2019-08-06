package com.abc.vo;

import com.abc.constant.ConfigType;
import com.abc.entity.CommonConfig;
import com.abc.util.kafka.KafkaTopic;
import com.abc.vo.commonconfigvoproperty.*;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CommonConfigVo extends CommonConfig implements PageVo {
    private int current;
    private int size;

    private DataBaseConfig dataBaseConfig;
    private DbQueryConfig dbQueryConfig;
    private HttpConfig httpConfig;
    private WsConfig wsConfig;
    private KafkaTopic kafkaTopic;
    private KafkaConsumerConfig consumerConfig;
    private KafkaProducerConfig producerConfig;

    public CommonConfigVo(CommonConfig commonConfig) {
        if (commonConfig.getType().equals(ConfigType.DB_CONFIG.getValue())) {
            setDataBaseConfig(JSON.parseObject(commonConfig.getValue(), DataBaseConfig.class));
        } else if (commonConfig.getType().equals(ConfigType.DB_QUERY_CONFIG.getValue())) {
            setDbQueryConfig(JSON.parseObject(commonConfig.getValue(), DbQueryConfig.class));
        } else if (commonConfig.getType().equals(ConfigType.HTTP_CONFIG.getValue())) {
            setHttpConfig(JSON.parseObject(commonConfig.getValue(), HttpConfig.class));
        } else if (commonConfig.getType().equals(ConfigType.WS_CONFIG.getValue())) {
            setWsConfig(JSON.parseObject(commonConfig.getValue(), WsConfig.class));
        } else if (commonConfig.getType().equals(ConfigType.KAFKA_TOPIC_INFO.getValue())) {
            setKafkaTopic(JSON.parseObject(commonConfig.getValue(), KafkaTopic.class));
        } else if (commonConfig.getType().equals(ConfigType.KAFKA_TOPIC_CONSUMER_INFO.getValue())) {
            setConsumerConfig(JSON.parseObject(commonConfig.getValue(), KafkaConsumerConfig.class));
        } else if (commonConfig.getType().equals(ConfigType.KAFKA_TOPIC_PRODUCER_INFO.getValue())) {
            setProducerConfig(JSON.parseObject(commonConfig.getValue(), KafkaProducerConfig.class));
        }
        try {
            BeanUtils.copyProperties(this, commonConfig);
        } catch (Exception e) {
            //do nothing
        }

        this.setValue(null);
    }

    public CommonConfig toEntity() {
        CommonConfig commonConfig = new CommonConfig();
        try {
            BeanUtils.copyProperties(commonConfig, this);
        } catch (Exception e) {
            //do nothing
        }
        if (commonConfig.getType().equals(ConfigType.DB_CONFIG.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getDataBaseConfig()));
        } else if (commonConfig.getType().equals(ConfigType.DB_QUERY_CONFIG.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getDbQueryConfig()));
        } else if (commonConfig.getType().equals(ConfigType.HTTP_CONFIG.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getHttpConfig()));
        } else if (commonConfig.getType().equals(ConfigType.WS_CONFIG.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getWsConfig()));
        } else if (commonConfig.getType().equals(ConfigType.KAFKA_TOPIC_INFO.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getKafkaTopic()));
        } else if (commonConfig.getType().equals(ConfigType.KAFKA_TOPIC_CONSUMER_INFO.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getConsumerConfig()));
        } else if (commonConfig.getType().equals(ConfigType.KAFKA_TOPIC_PRODUCER_INFO.getValue())) {
            commonConfig.setValue(JSON.toJSONString(getProducerConfig()));
        }
        return commonConfig;
    }
}
