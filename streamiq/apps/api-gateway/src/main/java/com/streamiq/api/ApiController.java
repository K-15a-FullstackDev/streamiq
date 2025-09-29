package main.java.com.streamiq.api;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final JdbcTemplate jdbc;
  private final SseHub hub;

  public ApiController(JdbcTemplate jdbc, SseHub hub) {
    this.jdbc = jdbc; this.hub = hub;
  }

  @GetMapping("/alerts")
  public List<Map<String,Object>> latest(@RequestParam(defaultValue = "50") int limit) {
    return jdbc.queryForList("""
      SELECT event_id, ts, source, level, score, anomaly, enrichment, classification
      FROM alerts ORDER BY ts DESC LIMIT ?
      """, limit);
  }

  @GetMapping("/stats")
  public Map<String,Object> stats() {
    var total = jdbc.queryForObject("SELECT count(*) FROM alerts", Long.class);
    var last15 = jdbc.queryForObject("SELECT count(*) FROM alerts WHERE ts > now() - interval '15 minutes'", Long.class);
    var crit = jdbc.queryForObject("SELECT count(*) FROM alerts WHERE level='critical' AND ts > now() - interval '15 minutes'", Long.class);
    return Map.of("total", total, "last15m", last15, "critical15m", crit);
  }

  @GetMapping(path="/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> stream() {
    return hub.stream().map(s -> "data: " + s + "\n\n").delayElements(Duration.ofMillis(0));
  }
}
