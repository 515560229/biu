package com.abc.vo.commonconfigvoproperty;

import lombok.Data;

@Data
public class KafkaTopicConfig extends KafkaClusterConfig {
    public static final String VERSION_0_8 = "0.8";
    public static final String VERSION_1_1 = "1.1";

    private String topic;//主题名称
    private String keyword;//关键字搜索,支持正则,最多返回100条记录
}
