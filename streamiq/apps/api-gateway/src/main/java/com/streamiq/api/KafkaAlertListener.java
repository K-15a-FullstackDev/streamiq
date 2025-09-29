package main.java.com.streamiq.api;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaAlertListener {
  private final SseHub hub;
  private final AlertRepo repo;

  public KafkaAlertListener(SseHub hub, AlertRepo repo) {
    this.hub = hub;
    this.repo = repo;
  }

  @KafkaListener(topics = "alerts.v1", groupId = "${spring.kafka.consumer.group-id}")
  public void onAlert(String value) {
    
    repo.insertAlertJson(value);
    hub.publish(value);
  }
}
