package com.streamiq.enricher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EnrichmentListener {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper om = new ObjectMapper();

  @Value("${streamiq.topic.out}") private String outTopic;

  public EnrichmentListener(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @KafkaListener(topics = "${streamiq.topic.in}", groupId = "${spring.kafka.consumer.group-id}")
  public void onMessage(ConsumerRecord<String, String> rec) throws Exception {
    Map<String, Object> ev = om.readValue(rec.value(), Map.class);

    Map<String, Object> enrichment = new HashMap<>();
    // naive geo by user/device suffix, just to show enrichment field
    String source = (String) ev.getOrDefault("source", "unknown");
    enrichment.put("geo", "US-TX");
    enrichment.put("sentiment", source.equals("twitter") ? 0.2 : 0.0);
    enrichment.put("device_reputation", source.equals("sensor") ? 0.8 : 0.5);

    ev.put("enrichment", enrichment);

    kafkaTemplate.send(outTopic, rec.key(), om.writeValueAsString(ev));
  }
}
