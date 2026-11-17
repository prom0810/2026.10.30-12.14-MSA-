package se.magnus.api.event;

import static java.time.ZonedDateTime.now;

import java.time.ZonedDateTime;

public class Event<K, T> {

  public enum Type {
    CREATE,
    DELETE
  }

  private final Type eventType;
  private final K key;
  private final T data;
  private final ZonedDateTime eventCreatedAt;

  public Event() {
    this.eventType = null;
    this.key = null;
    this.data = null;
    this.eventCreatedAt = null;
  }

  public Event(Type eventType, K key, T data) {
    this(eventType, key, data, now());
  }

  @com.fasterxml.jackson.annotation.JsonCreator
  public Event(
      @com.fasterxml.jackson.annotation.JsonProperty("eventType") Type eventType,
      @com.fasterxml.jackson.annotation.JsonProperty("key") K key,
      @com.fasterxml.jackson.annotation.JsonProperty("data") T data,
      @com.fasterxml.jackson.annotation.JsonProperty("eventCreatedAt") ZonedDateTime eventCreatedAt) {
    this.eventType = eventType;
    this.key = key;
    this.data = data;
    this.eventCreatedAt = eventCreatedAt;
  }

  public Type getEventType() {
    return eventType;
  }

  public K getKey() {
    return key;
  }

  public T getData() {
    return data;
  }
  public ZonedDateTime getEventCreatedAt() {
    return eventCreatedAt;
  }
}

