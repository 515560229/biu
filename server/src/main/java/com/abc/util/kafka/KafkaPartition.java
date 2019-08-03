package com.abc.util.kafka;

import com.google.common.net.HostAndPort;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class KafkaPartition {
  private int id;
  private String topicName;
  private KafkaLeader leader;

  public static class Builder {
    private int id = 0;
    private String topicName = "";
    private int leaderId = 0;
    private HostAndPort leaderHostAndPort;

    public Builder withId(int id) {
      this.id = id;
      return this;
    }

    public Builder withTopicName(String topicName) {
      this.topicName = topicName;
      return this;
    }

    public Builder withLeaderId(int leaderId) {
      this.leaderId = leaderId;
      return this;
    }

    public Builder withLeaderHostAndPort(String hostPortString) {
      this.leaderHostAndPort = HostAndPort.fromString(hostPortString);
      return this;
    }

    public Builder withLeaderHostAndPort(String host, int port) {
      this.leaderHostAndPort = HostAndPort.fromParts(host, port);
      return this;
    }

    public KafkaPartition build() {
      return new KafkaPartition(this);
    }
  }

  public KafkaPartition(KafkaPartition other) {
    this.topicName = other.topicName;
    this.id = other.id;
    this.leader = new KafkaLeader(other.leader.id, other.leader.geetHostAndPort());
  }

  private KafkaPartition(Builder builder) {
    this.id = builder.id;
    this.topicName = builder.topicName;
    this.leader = new KafkaLeader(builder.leaderId, builder.leaderHostAndPort);
  }

  public void setLeaderByPameters(int leaderId, String leaderHost, int leaderPort) {
    this.leader = new KafkaLeader(leaderId, HostAndPort.fromParts(leaderHost, leaderPort));
  }

  @Data
  @NoArgsConstructor
  public final static class KafkaLeader {
    private int id;
    private String host;
    private int port;

    public KafkaLeader(int id, HostAndPort hostAndPort) {
      this.id = id;
      this.host = hostAndPort.getHost();
      this.port = hostAndPort.getPort();
    }

    public HostAndPort geetHostAndPort() {
      return HostAndPort.fromString(String.format("%s:%S", host, port));
    }
  }

}