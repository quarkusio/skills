package org.eclipse.cargotracker.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.Delivery;
import org.eclipse.cargotracker.domain.model.cargo.Itinerary;
import org.eclipse.cargotracker.domain.model.cargo.RoutingStatus;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.cargo.TransportStatus;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaCargoRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application layer integration test covering a number of otherwise fairly trivial components that
 * largely do not warrant their own tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookingServiceTest {
  private static TrackingId trackingId;
  private static List<Itinerary> candidates;
  private static LocalDate deadline;
  private static Itinerary assigned;

  @Autowired
  private BookingService bookingService;
  @Autowired
  private JpaCargoRepository cargoRepository;

  @Test
  @Order(1)
  @Transactional
  @Commit
  public void testRegisterNew() {
    UnLocode fromUnlocode = new UnLocode("USCHI");
    UnLocode toUnlocode = new UnLocode("SESTO");

    deadline = LocalDate.now().plusMonths(6);

    trackingId = bookingService.bookNewCargo(fromUnlocode, toUnlocode, deadline);

    Cargo cargo = cargoRepository.findByTrackingId(trackingId).orElseThrow();

    assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
    assertEquals(SampleLocations.STOCKHOLM, cargo.getRouteSpecification().getDestination());
    assertTrue(deadline.isEqual(cargo.getRouteSpecification().getArrivalDeadline()));
    assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
    assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
    assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
    assertFalse(cargo.getDelivery().isMisdirected());
    assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
    assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
    assertFalse(cargo.getDelivery().isUnloadedAtDestination());
    assertEquals(RoutingStatus.NOT_ROUTED, cargo.getDelivery().getRoutingStatus());
    assertEquals(Itinerary.EMPTY_ITINERARY, cargo.getItinerary());
  }

  @Test
  @Order(2)
  @Transactional
  @Commit
  public void testRouteCandidates() {
    candidates = bookingService.requestPossibleRoutesForCargo(trackingId);

    assertFalse(candidates.isEmpty());
  }

  @Test
  @Order(3)
  @Transactional
  @Commit
  public void testAssignRoute() {
    assigned = candidates.get(new Random().nextInt(candidates.size()));

    bookingService.assignCargoToRoute(assigned, trackingId);

    Cargo cargo = cargoRepository.findByTrackingId(trackingId).orElseThrow();

    assertEquals(assigned, cargo.getItinerary());
    assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
    assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
    assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
    assertFalse(cargo.getDelivery().isMisdirected());
    assertTrue(cargo.getDelivery().getEstimatedTimeOfArrival().isBefore(deadline.atStartOfDay()));
    assertEquals(
        HandlingEvent.Type.RECEIVE, cargo.getDelivery().getNextExpectedActivity().getType());
    assertEquals(
        SampleLocations.CHICAGO, cargo.getDelivery().getNextExpectedActivity().getLocation());
    assertEquals(null, cargo.getDelivery().getNextExpectedActivity().getVoyage());
    assertFalse(cargo.getDelivery().isUnloadedAtDestination());
    assertEquals(RoutingStatus.ROUTED, cargo.getDelivery().getRoutingStatus());
  }

  @Test
  @Order(4)
  @Transactional
  @Commit
  public void testChangeDestination() {
    bookingService.changeDestination(trackingId, new UnLocode("FIHEL"));

    Cargo cargo = cargoRepository.findByTrackingId(trackingId).orElseThrow();

    assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
    assertEquals(SampleLocations.HELSINKI, cargo.getRouteSpecification().getDestination());
    assertTrue(deadline.isEqual(cargo.getRouteSpecification().getArrivalDeadline()));
    assertEquals(assigned, cargo.getItinerary());
    assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
    assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
    assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
    assertFalse(cargo.getDelivery().isMisdirected());
    assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
    assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
    assertFalse(cargo.getDelivery().isUnloadedAtDestination());
    assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery().getRoutingStatus());
  }

  @Test
  @Order(5)
  @Transactional
  @Commit
  public void testChangeDeadline() {
    LocalDate newDeadline = deadline.plusMonths(1);
    bookingService.changeDeadline(trackingId, newDeadline);

    Cargo cargo = cargoRepository.findByTrackingId(trackingId).orElseThrow();

    assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
    assertEquals(SampleLocations.HELSINKI, cargo.getRouteSpecification().getDestination());
    assertTrue(newDeadline.isEqual(cargo.getRouteSpecification().getArrivalDeadline()));
    assertEquals(assigned, cargo.getItinerary());
    assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
    assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
    assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
    assertFalse(cargo.getDelivery().isMisdirected());
    assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
    assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
    assertFalse(cargo.getDelivery().isUnloadedAtDestination());
    assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery().getRoutingStatus());
  }
}
