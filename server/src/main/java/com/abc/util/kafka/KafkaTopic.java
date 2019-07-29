package com.abc.util.kafka;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

public final class KafkaTopic {
  private final String name;
  private final List<KafkaPartition> partitions;

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