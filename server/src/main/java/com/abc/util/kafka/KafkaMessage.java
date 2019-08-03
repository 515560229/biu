package com.abc.util.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaMessage {
    private int partition;
    private long offset;
    private String key;
    private String message;
    private List<Map<String, Object>> header;
}