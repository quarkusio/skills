package org.eclipse.cargotracker.infrastructure.messaging.jms;

import org.eclipse.cargotracker.application.CargoInspectionService;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes JMS messages and delegates notification of misdirected cargo to the tracking service.
 *
 * <p>
 * This is a programmatic hook into the JMS infrastructure to make cargo inspection message-driven.
 */
@Component
public class CargoHandledConsumer {

  private CargoInspectionService cargoInspectionService;

  public CargoHandledConsumer(CargoInspectionService cargoInspectionService) {
    this.cargoInspectionService = cargoInspectionService;
  }

  @JmsListener(destination = "${cargo.handled.queue}")
  public void onMessage(String trackingId) {
    cargoInspectionService.inspectCargo(new TrackingId(trackingId));
  }
}
