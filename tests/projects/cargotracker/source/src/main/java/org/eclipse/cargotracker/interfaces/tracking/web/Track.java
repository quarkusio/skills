package org.eclipse.cargotracker.interfaces.tracking.web;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.view.ViewScoped;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaCargoRepository;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaHandlingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Backing bean for tracking cargo. This interface sits immediately on top of the domain layer,
 * unlike the booking interface which has a facade and supporting DTOs in between.
 *
 * <p>
 * An adapter class, designed for the tracking use case, is used to wrap the domain model to make it
 * easier to work with in a web page rendering context. We do not want to apply view rendering
 * constraints to the design of our domain model and the adapter helps us shield the domain model
 * classes where needed.
 *
 * <p>
 * In some very simplistic cases, it is fine to not use even an adapter.
 */
@Component("publicTrack")
@ViewScoped
public class Track implements Serializable {

  private static final long serialVersionUID = 1L;

  @Autowired
  private transient Logger logger;

  @Autowired
  private JpaCargoRepository cargoRepository;
  @Autowired
  private JpaHandlingEventRepository handlingEventRepository;

  private String trackingId;
  private CargoTrackingViewAdapter cargo;

  public String getTrackingId() {
    return trackingId;
  }

  public void setTrackingId(String trackingId) {
    if (trackingId != null) {
      trackingId = trackingId.trim();
    }

    this.trackingId = trackingId;
  }

  public CargoTrackingViewAdapter getCargo() {
    return cargo;
  }

  public String getCargoAsJson() {
    try {
      ObjectMapper om = new ObjectMapper();
      return URLEncoder.encode(om.writeValueAsString(cargo), UTF_8.name());
    } catch (UnsupportedEncodingException | JsonProcessingException ex) {
      logger.log(Level.WARNING, "URL encoding error.", ex);
      return ""; // Should never happen.
    }
  }

  public void onTrackById() {
    Cargo cargo = cargoRepository.findByTrackingId(new TrackingId(trackingId)).orElse(null);

    if (cargo != null) {
      List<HandlingEvent> handlingEvents =
          handlingEventRepository
              .lookupHandlingHistoryOfCargo(new TrackingId(trackingId))
              .getDistinctEventsByCompletionTime();
      this.cargo = new CargoTrackingViewAdapter(cargo, handlingEvents);
    } else {
      this.cargo = null;
    }
  }
}
