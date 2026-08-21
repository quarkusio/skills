package org.eclipse.cargotracker.infrastructure.messaging.jms;

import org.eclipse.cargotracker.application.HandlingEventService;
import org.eclipse.cargotracker.domain.model.handling.CannotCreateHandlingEventException;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/** Consumes handling event registration attempt messages and delegates to proper registration. */
@Component
public class HandlingEventRegistrationAttemptConsumer {

  private HandlingEventService handlingEventService;

  public HandlingEventRegistrationAttemptConsumer(HandlingEventService handlingEventService) {
    this.handlingEventService = handlingEventService;
  }

  @JmsListener(destination = "${handling.event.registration.attempt.queue}")
  public void onMessage(HandlingEventRegistrationAttempt attempt) {
    try {
      handlingEventService.registerHandlingEvent(
          attempt.getCompletionTime(),
          attempt.getTrackingId(),
          attempt.getVoyageNumber(),
          attempt.getUnLocode(),
          attempt.getType());
    } catch (CannotCreateHandlingEventException e) {
      // Poison messages will be placed on dead-letter queue.
      throw new RuntimeException("Error occurred processing message", e);
    }
  }
}
