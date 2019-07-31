package com.abc.util.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class KafkaMessage {
    private String key;
    private String message;
    private List<Map<String, Object>> header;
}