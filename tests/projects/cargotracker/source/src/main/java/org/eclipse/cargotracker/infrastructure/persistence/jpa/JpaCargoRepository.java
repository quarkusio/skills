package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import java.util.Optional;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCargoRepository extends JpaRepository<Cargo, Long>, CargoRepository {

    Optional<Cargo> findByTrackingId(TrackingId trackingId);
}
