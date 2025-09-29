package main.java.com.streamiq.api;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;

@Component
public class SseHub {
  private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
  public void publish(String json) { sink.tryEmitNext(json); }
  public Flux<String> stream() { return sink.asFlux(); }
}
