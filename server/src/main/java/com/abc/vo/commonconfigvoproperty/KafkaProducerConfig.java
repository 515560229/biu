package com.abc.vo.commonconfigvoproperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class KafkaProducerConfig extends KafkaClusterConfig {
    private String topic;//要发送的主题名称
    private String message;//要发送的消息模板
    private List<Parameter> parameters;//参数列表
}
