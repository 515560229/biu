package com.abc.util.kafka.examples;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KafkaConfig {
    // clusterName -> 192.168.1.1:9092,192.168.1.1:9093,192.168.1.1:9094
    private Map<String, String> cluster;
    private List<Config> clusterConfigList;

    public void setCluster(Map<String, String> cluster) {
        this.cluster = cluster;
        if (cluster != null) {
            clusterConfigList = new ArrayList<>(cluster.size());
            for (Map.Entry<String, String> entry : cluster.entrySet()) {
                clusterConfigList.add(new Config(entry.getKey(), Arrays.asList(entry.getValue().split(","))));
            }
        }
    }

    public List<Config> getClusterConfigList() {
        return this.clusterConfigList;
    }

    @Data
    @AllArgsConstructor
    public static final class Config {
        private String clusterName;
        private List<String> brokerList;
    }
}
