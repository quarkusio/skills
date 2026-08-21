package org.eclipse.cargotracker.application;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.eclipse.cargotracker.domain.model.cargo.Itinerary;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.springframework.validation.annotation.Validated;

/** Cargo booking service. */
public interface BookingService {

  /** Registers a new cargo in the tracking system, not yet routed. */
  TrackingId bookNewCargo(
      @NotNull(message = "Origin is required.") @Validated UnLocode origin,
      @NotNull(message = "Destination is required.") @Validated UnLocode destination,
      @NotNull(message = "Deadline is required.") @Future(
          message = "Deadline must be in the future.") LocalDate arrivalDeadline);

  /**
   * Requests a list of itineraries describing possible routes for this cargo.
   *
   * @param trackingId Cargo tracking ID
   * @return A list of possible itineraries for this cargo
   */
  List<Itinerary> requestPossibleRoutesForCargo(
      @NotNull(message = "Tracking ID is required.") @Validated TrackingId trackingId);

  void assignCargoToRoute(
      @NotNull(message = "Itinerary is required.") @Validated Itinerary itinerary,
      @NotNull(message = "Tracking ID is required.") @Validated TrackingId trackingId);

  void changeDestination(
      @NotNull(message = "Tracking ID is required.") @Validated TrackingId trackingId,
      @NotNull(message = "Destination is required.") @Validated UnLocode unLocode);

  void changeDeadline(
      @NotNull(message = "Tracking ID is required.") @Validated TrackingId trackingId,
      @NotNull(message = "Deadline is required.") @Future(
          message = "Deadline must be in the future.") LocalDate deadline);
}
