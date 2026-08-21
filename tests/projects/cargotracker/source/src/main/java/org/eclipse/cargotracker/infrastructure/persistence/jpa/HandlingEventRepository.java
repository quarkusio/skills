package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingHistory;

public interface HandlingEventRepository {

  HandlingHistory lookupHandlingHistoryOfCargo(TrackingId trackingId);
}
