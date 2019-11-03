package com.abc.constant;

public enum ConfigType {
    DB_CONFIG("db"),
    DB_QUERY_CONFIG("dbQuery"),
    HTTP_CONFIG("http"),
    WS_CONFIG("ws"),
    KAFKA_CLUSTER_INFO("kafClus"),
    KAFKA_TOPIC_INFO("kafka"),
    KAFKA_TOPIC_CONSUMER_INFO("consumer"),
    KAFKA_TOPIC_PRODUCER_INFO("producer"),
    ZOOKEEPER_CLUSTER_INFO("zkClus"),
    ZOOKEEPER_QUERY("zkQuery")
    ;

    private String value;

    private ConfigType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
