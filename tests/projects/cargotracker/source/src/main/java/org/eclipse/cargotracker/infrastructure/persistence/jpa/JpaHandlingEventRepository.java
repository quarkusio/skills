package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaHandlingEventRepository
    extends JpaRepository<HandlingEvent, Long>, HandlingEventRepository {

}
