package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;

public interface CargoRepository {

  void store(Cargo cargo);

  TrackingId nextTrackingId();
}
