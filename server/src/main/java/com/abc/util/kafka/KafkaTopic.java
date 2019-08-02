package com.abc.util.kafka;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public final class KafkaTopic {
  private String name;
  private List<KafkaPartition> partitions;
  private String version;

  public KafkaTopic(String name, List<KafkaPartition> partitions) {
    this.name = name;
    this.partitions = Lists.newArrayList();
    for (KafkaPartition partition : partitions) {
      this.partitions.add(new KafkaPartition(partition));
    }
  }

  public String getName() {
    return this.name;
  }

  public List<KafkaPartition> getPartitions() {
    return Collections.unmodifiableList(this.partitions);
  }

}