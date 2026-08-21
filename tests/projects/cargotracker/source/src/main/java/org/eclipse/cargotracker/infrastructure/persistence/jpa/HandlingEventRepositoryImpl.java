package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.handling.HandlingHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class HandlingEventRepositoryImpl implements HandlingEventRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public HandlingHistory lookupHandlingHistoryOfCargo(TrackingId trackingId) {
    return new HandlingHistory(
        entityManager
            .createNamedQuery("HandlingEvent.findByTrackingId", HandlingEvent.class)
            .setParameter("trackingId", trackingId)
            .getResultList());
  }
}
