package org.eclipse.cargotracker.application.internal;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.cargotracker.application.ApplicationEvents;
import org.eclipse.cargotracker.application.CargoInspectionService;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingHistory;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaCargoRepository;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaHandlingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCargoInspectionService implements CargoInspectionService {

  private Logger logger;
  private ApplicationEvents applicationEvents;
  private JpaCargoRepository cargoRepository;
  private JpaHandlingEventRepository handlingEventRepository;

  public DefaultCargoInspectionService(Logger logger,
      ApplicationEvents applicationEvents,
      JpaCargoRepository cargoRepository,
      JpaHandlingEventRepository handlingEventRepository) {
    this.logger = logger;
    this.applicationEvents = applicationEvents;
    this.cargoRepository = cargoRepository;
    this.handlingEventRepository = handlingEventRepository;
  }

  @Override
  public void inspectCargo(TrackingId trackingId) {
    Cargo cargo = cargoRepository.findByTrackingId(trackingId).orElse(null);

    if (cargo == null) {
      logger.log(Level.WARNING, "Can't inspect non-existing cargo {0}", trackingId);
      return;
    }

    HandlingHistory handlingHistory =
        handlingEventRepository.lookupHandlingHistoryOfCargo(trackingId);

    cargo.deriveDeliveryProgress(handlingHistory);

    if (cargo.getDelivery().isMisdirected()) {
      applicationEvents.cargoWasMisdirected(cargo);
    }

    if (cargo.getDelivery().isUnloadedAtDestination()) {
      applicationEvents.cargoHasArrived(cargo);
    }

    cargoRepository.store(cargo);
  }
}
