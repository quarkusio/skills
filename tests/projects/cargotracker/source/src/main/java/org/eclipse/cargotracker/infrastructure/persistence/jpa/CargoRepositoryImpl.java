package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

public class CargoRepositoryImpl implements CargoRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Override
  public void store(Cargo cargo) {
    // TODO [Clean Code] See why cascade is not working correctly for legs.
    cargo.getItinerary().getLegs().forEach(leg -> entityManager.persist(leg));

    entityManager.persist(cargo);

    eventPublisher.publishEvent(cargo);
  }

  @Override
  public TrackingId nextTrackingId() {
    String random = UUID.randomUUID().toString().toUpperCase();

    return new TrackingId(random.substring(0, random.indexOf("-")));
  }
}
