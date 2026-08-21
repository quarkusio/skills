package org.eclipse.cargotracker.infrastructure.messaging.jms;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class RejectedRegistrationAttemptsConsumer {

  private Logger logger;

  public RejectedRegistrationAttemptsConsumer(Logger logger) {
    this.logger = logger;
  }

  @JmsListener(destination = "${rejected.registration.attempts.queue}")
  public void onMessage(String trackingId) {
    logger.log(
        Level.INFO,
        "Rejected registration attempt of cargo with tracking ID {0}.",
        trackingId);
  }
}
