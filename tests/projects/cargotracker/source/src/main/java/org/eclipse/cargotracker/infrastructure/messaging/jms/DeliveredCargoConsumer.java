package org.eclipse.cargotracker.infrastructure.messaging.jms;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveredCargoConsumer {

  private Logger logger;

  public DeliveredCargoConsumer(Logger logger) {
    this.logger = logger;
  }

  @JmsListener(destination = "${cargo.delivered.queue}")
  public void onMessage(String trackingId) {
    logger.log(
        Level.INFO, "Cargo with tracking ID {0} delivered.", trackingId);
  }
}
