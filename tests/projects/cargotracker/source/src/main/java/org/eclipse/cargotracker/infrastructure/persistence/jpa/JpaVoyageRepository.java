package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import java.util.List;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaVoyageRepository extends JpaRepository<Voyage, Long> {

  Voyage findByVoyageNumber(VoyageNumber voyageNumber);

  List<Voyage> findAllByOrderByVoyageNumberAsc();

}
