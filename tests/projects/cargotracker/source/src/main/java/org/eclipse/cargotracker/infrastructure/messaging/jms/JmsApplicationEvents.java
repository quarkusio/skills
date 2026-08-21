package org.eclipse.cargotracker.infrastructure.messaging.jms;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.cargotracker.application.ApplicationEvents;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class JmsApplicationEvents implements ApplicationEvents {

  @Value("${cargo.handled.queue}")
  private String cargoHandledQueue;
  @Value("${misdirected.cargo.queue}")
  private String misdirectedCargoQueue;
  @Value("${cargo.delivered.queue}")
  private String deliveredCargoQueue;
  @Value("${handling.event.registration.attempt.queue}")
  private String handlingEventQueue;

  private JmsTemplate jmsTemplate;
  private Logger logger;

  public JmsApplicationEvents(JmsTemplate jmsTemplate, Logger logger) {
    this.jmsTemplate = jmsTemplate;
    this.logger = logger;
  }

  @Override
  public void cargoWasHandled(HandlingEvent event) {
    Cargo cargo = event.getCargo();
    logger.log(Level.INFO, "Cargo was handled {0}", cargo);
    jmsTemplate.convertAndSend(cargoHandledQueue, cargo.getTrackingId().getIdString());
  }

  @Override
  public void cargoWasMisdirected(Cargo cargo) {
    logger.log(Level.INFO, "Cargo was misdirected {0}", cargo);
    jmsTemplate.convertAndSend(misdirectedCargoQueue, cargo.getTrackingId().getIdString());
  }

  @Override
  public void cargoHasArrived(Cargo cargo) {
    logger.log(Level.INFO, "Cargo has arrived {0}", cargo);
    jmsTemplate.convertAndSend(deliveredCargoQueue, cargo.getTrackingId().getIdString());
  }

  @Override
  public void receivedHandlingEventRegistrationAttempt(HandlingEventRegistrationAttempt attempt) {
    logger.log(Level.INFO, "Received handling event registration attempt {0}", attempt);
    // ugly, just for compatibility
    long ttl = jmsTemplate.getTimeToLive();
    jmsTemplate.setTimeToLive(1000);
    try {
      jmsTemplate.convertAndSend(handlingEventQueue, attempt);
    } finally {
      jmsTemplate.setTimeToLive(ttl);
    }
  }
}
