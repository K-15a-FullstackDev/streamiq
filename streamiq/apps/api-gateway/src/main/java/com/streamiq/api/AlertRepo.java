package main.java.com.streamiq.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Repository
public class AlertRepo {
  private final JdbcTemplate jdbc;
  private final ObjectMapper om = new ObjectMapper();

  public AlertRepo(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public void insertAlertJson(String json) {
    try {
      @SuppressWarnings("unchecked")
      Map<String,Object> ev = om.readValue(json, Map.class);

      String eventId = (String) ev.getOrDefault("event_id", "");
      String ts      = (String) ev.getOrDefault("ts", "");
      String source  = (String) ev.getOrDefault("source", "unknown");

      @SuppressWarnings("unchecked")
      Map<String,Object> anomaly = (Map<String,Object>) ev.getOrDefault("anomaly", Map.of());

      String level = (String) anomaly.getOrDefault("level", "low");
      double score = toDouble(anomaly.get("score"));

      jdbc.update("""
        INSERT INTO alerts(event_id, ts, source, level, score, payload, enrichment, classification, anomaly)
        VALUES (?, ?::timestamptz, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb))
        """,
        new Object[] {
          eventId, ts, source, level, score,
          jsonField(ev.get("payload")),
          jsonField(ev.get("enrichment")),
          jsonField(ev.get("classification")),
          jsonField(anomaly)
        }
      );
    } catch (Exception e) {
      // swallow insert errors; API is best-effort fanout + persist
    }
  }

  private String jsonField(Object o) {
    try { return o == null ? "{}" : om.writeValueAsString(o); }
    catch (Exception e) { return "{}"; }
  }

  private double toDouble(Object o) {
    if (o instanceof Number n) return n.doubleValue();
    try { return o == null ? 0.0 : Double.parseDouble(o.toString()); }
    catch (Exception e) { return 0.0; }
  }
}
