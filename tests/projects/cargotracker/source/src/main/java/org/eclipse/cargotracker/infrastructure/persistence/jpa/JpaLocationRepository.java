package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaLocationRepository extends JpaRepository<Location, Long> {

  Location findByUnLocode(UnLocode unLocode);

}
