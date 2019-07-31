package com.abc.vo.commonconfigvoproperty;

import lombok.Data;

@Data
public class KafkaClusterConfig {
    public static final String VERSION_0_8 = "0.8";
    public static final String VERSION_1_1 = "1.1";

    private String zkConnect;//localhost:2181
    private String broker;//localhost:9092
    private String version;//0.8  1.1
    private String clusterName;//集群名称
}
