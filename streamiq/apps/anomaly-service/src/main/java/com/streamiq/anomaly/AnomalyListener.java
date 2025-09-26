package com.streamiq.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AnomalyListener {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper om = new ObjectMapper();

  @Value("${streamiq.topic.out}") private String outTopic;

  public AnomalyListener(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @KafkaListener(topics = "${streamiq.topic.in}", groupId = "${spring.kafka.consumer.group-id}")
  public void onMessage(ConsumerRecord<String, String> rec) throws Exception {
    Map<String, Object> ev = om.readValue(rec.value(), Map.class);

    // Compute a simple risk score using enrichment + payload
    double score = 0.0;
    Map<String, Object> payload = (Map<String, Object>) ev.getOrDefault("payload", Map.of());
    Map<String, Object> enrichment = (Map<String, Object>) ev.getOrDefault("enrichment", Map.of());

    Object temp = payload.get("temp");
    Object humidity = payload.get("humidity");
    if (temp instanceof Number t && t.doubleValue() > 32) score += 1.0;
    if (humidity instanceof Number h && h.doubleValue() > 75) score += 0.8;

    Object sentiment = enrichment.get("sentiment");
    if (sentiment instanceof Number s && s.doubleValue() < -0.2) score += 0.6;

    String level = score >= 1.3 ? "critical"
                 : score >= 0.9 ? "high"
                 : score >= 0.5 ? "medium"
                 : "low";

    Map<String, Object> anomaly = Map.of(
      "score", Math.round(score * 100.0) / 100.0,
      "level", level,
      "method", "rule_v1"
    );

    ev.put("anomaly", anomaly);

    kafkaTemplate.send(outTopic, rec.key(), om.writeValueAsString(ev));
  }
}
