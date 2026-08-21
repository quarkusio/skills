package org.eclipse.cargotracker.interfaces.handling.rest;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import org.eclipse.cargotracker.application.ApplicationEvents;
import org.eclipse.cargotracker.application.util.DateConverter;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This REST end-point implementation performs basic validation and parsing of incoming data, and in
 * case of a valid registration attempt, sends an asynchronous message with the information to the
 * handling event registration system for proper registration.
 */
@RestController
@RequestMapping("/rest/handling")
@Transactional
public class HandlingReportService {

  private ApplicationEvents applicationEvents;

  public HandlingReportService(
      ApplicationEvents applicationEvents) {
    this.applicationEvents = applicationEvents;
  }

  @PostMapping(value = "/reports",
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public void submitReport(
      @NotNull(
          message = "Missing handling report.") @Validated @RequestBody HandlingReport handlingReport) {
    LocalDateTime completionTime = DateConverter.toDateTime(handlingReport.getCompletionTime());
    VoyageNumber voyageNumber = null;

    if (handlingReport.getVoyageNumber() != null) {
      voyageNumber = new VoyageNumber(handlingReport.getVoyageNumber());
    }

    HandlingEvent.Type type = HandlingEvent.Type.valueOf(handlingReport.getEventType());
    UnLocode unLocode = new UnLocode(handlingReport.getUnLocode());

    TrackingId trackingId = new TrackingId(handlingReport.getTrackingId());

    HandlingEventRegistrationAttempt attempt =
        new HandlingEventRegistrationAttempt(
            LocalDateTime.now(), completionTime, trackingId, voyageNumber, type, unLocode);

    applicationEvents.receivedHandlingEventRegistrationAttempt(attempt);
  }
}
