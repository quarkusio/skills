package org.eclipse.cargotracker.interfaces.booking.sse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaCargoRepository;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Sever-sent events service for tracking all cargo in real time. */
@RestController
@RequestMapping("/rest/cargo")
public class RealtimeCargoTrackingService {

  private Logger logger;
  private JpaCargoRepository cargoRepository;

  private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

  public RealtimeCargoTrackingService(Logger logger, JpaCargoRepository cargoRepository) {
    this.logger = logger;
    this.cargoRepository = cargoRepository;
  }

  @PreDestroy
  public void close() {
    emitters.forEach(SseEmitter::complete);
    emitters.clear();
    logger.log(Level.FINEST, "SSE emitters cleaned up.");
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter tracking() {
    SseEmitter emitter = new SseEmitter(0L); // disable timeout
    emitters.add(emitter);

    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));

    cargoRepository.findAll().forEach(cargo -> sendCargo(emitter, cargo));
    logger.log(Level.FINEST, "New SSE client connected and registered.");

    return emitter;
  }

  @Async
  @EventListener
  public void onCargoUpdated(Cargo cargo) {
    logger.log(Level.FINEST, "SSE event broadcast for cargo: {0}", cargo);
    emitters.forEach(emitter -> sendCargo(emitter, cargo));
  }

  private void sendCargo(SseEmitter emitter, Cargo cargo) {
    try {
      var payload = new RealtimeCargoTrackingViewAdapter(cargo);
      emitter.send(payload, MediaType.APPLICATION_JSON);
    } catch (IOException e) {
      emitter.completeWithError(e);
      emitters.remove(emitter);
      logger.log(Level.WARNING, "SSE emitter error, removed emitter:" + e.getMessage());
    }
  }

}
