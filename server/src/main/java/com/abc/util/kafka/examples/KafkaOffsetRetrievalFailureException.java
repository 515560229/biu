package com.abc.util.kafka.examples;

@SuppressWarnings("serial")
public class KafkaOffsetRetrievalFailureException extends Exception {

  public KafkaOffsetRetrievalFailureException(String message) {
    super(message);
  }

}