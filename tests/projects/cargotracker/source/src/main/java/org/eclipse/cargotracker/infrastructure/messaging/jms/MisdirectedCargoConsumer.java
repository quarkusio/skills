package org.eclipse.cargotracker.infrastructure.messaging.jms;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MisdirectedCargoConsumer {

  private Logger logger;

  public MisdirectedCargoConsumer(Logger logger) {
    this.logger = logger;
  }

  @JmsListener(destination = "${misdirected.cargo.queue}")
  public void onMessage(String trackingId) {
    logger.log(
        Level.INFO, "Cargo with tracking ID {0} misdirected.", trackingId);
  }

}
