package org.eclipse.cargotracker.interfaces.booking.facade.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.cargotracker.application.BookingService;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.Itinerary;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaCargoRepository;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaHandlingEventRepository;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaLocationRepository;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaVoyageRepository;
import org.eclipse.cargotracker.interfaces.booking.facade.BookingServiceFacade;
import org.eclipse.cargotracker.interfaces.booking.facade.dto.CargoRoute;
import org.eclipse.cargotracker.interfaces.booking.facade.dto.CargoStatus;
import org.eclipse.cargotracker.interfaces.booking.facade.dto.RouteCandidate;
import org.eclipse.cargotracker.interfaces.booking.facade.internal.assembler.CargoRouteDtoAssembler;
import org.eclipse.cargotracker.interfaces.booking.facade.internal.assembler.CargoStatusDtoAssembler;
import org.eclipse.cargotracker.interfaces.booking.facade.internal.assembler.ItineraryCandidateDtoAssembler;
import org.eclipse.cargotracker.interfaces.booking.facade.internal.assembler.LocationDtoAssembler;
import org.springframework.stereotype.Component;

@Component
public class DefaultBookingServiceFacade implements BookingServiceFacade {

  private BookingService bookingService;
  private JpaLocationRepository locationRepository;
  private JpaCargoRepository cargoRepository;
  private JpaVoyageRepository voyageRepository;
  private JpaHandlingEventRepository handlingEventRepository;
  private CargoRouteDtoAssembler cargoRouteDtoAssembler;
  private CargoStatusDtoAssembler cargoStatusDtoAssembler;
  private ItineraryCandidateDtoAssembler itineraryCandidateDtoAssembler;
  private LocationDtoAssembler locationDtoAssembler;

  public DefaultBookingServiceFacade(
      BookingService bookingService,
      JpaLocationRepository locationRepository,
      JpaCargoRepository cargoRepository,
      JpaVoyageRepository voyageRepository,
      JpaHandlingEventRepository handlingEventRepository,
      CargoRouteDtoAssembler cargoRouteDtoAssembler,
      CargoStatusDtoAssembler cargoStatusDtoAssembler,
      ItineraryCandidateDtoAssembler itineraryCandidateDtoAssembler,
      LocationDtoAssembler locationDtoAssembler) {
    this.bookingService = bookingService;
    this.locationRepository = locationRepository;
    this.cargoRepository = cargoRepository;
    this.voyageRepository = voyageRepository;
    this.handlingEventRepository = handlingEventRepository;
    this.cargoRouteDtoAssembler = cargoRouteDtoAssembler;
    this.cargoStatusDtoAssembler = cargoStatusDtoAssembler;
    this.itineraryCandidateDtoAssembler = itineraryCandidateDtoAssembler;
    this.locationDtoAssembler = locationDtoAssembler;
  }

  @Override
  public List<org.eclipse.cargotracker.interfaces.booking.facade.dto.Location> listShippingLocations() {
    List<Location> allLocations = locationRepository.findAll();
    return locationDtoAssembler.toDtoList(allLocations);
  }

  @Override
  public String bookNewCargo(String origin, String destination, LocalDate arrivalDeadline) {
    TrackingId trackingId =
        bookingService.bookNewCargo(
            new UnLocode(origin), new UnLocode(destination), arrivalDeadline);
    return trackingId.getIdString();
  }

  @Override
  public CargoRoute loadCargoForRouting(String trackingId) {
    Cargo cargo = cargoRepository.findByTrackingId(new TrackingId(trackingId)).orElse(null);
    return cargoRouteDtoAssembler.toDto(cargo);
  }

  @Override
  public void assignCargoToRoute(String trackingIdStr, RouteCandidate routeCandidateDTO) {
    Itinerary itinerary =
        itineraryCandidateDtoAssembler.fromDTO(
            routeCandidateDTO, voyageRepository, locationRepository);
    TrackingId trackingId = new TrackingId(trackingIdStr);

    bookingService.assignCargoToRoute(itinerary, trackingId);
  }

  @Override
  public void changeDestination(String trackingId, String destinationUnLocode) {
    bookingService.changeDestination(new TrackingId(trackingId), new UnLocode(destinationUnLocode));
  }

  @Override
  public void changeDeadline(String trackingId, LocalDate arrivalDeadline) {
    bookingService.changeDeadline(new TrackingId(trackingId), arrivalDeadline);
  }

  @Override
  // TODO [DDD] Is this the correct DTO here?
  public List<CargoRoute> listAllCargos() {
    List<Cargo> cargos = cargoRepository.findAll();
    List<CargoRoute> routes;

    routes = cargos.stream().map(cargoRouteDtoAssembler::toDto).collect(Collectors.toList());

    return routes;
  }

  @Override
  public List<String> listAllTrackingIds() {
    List<String> trackingIds = new ArrayList<>();
    cargoRepository
        .findAll()
        .forEach(cargo -> trackingIds.add(cargo.getTrackingId().getIdString()));

    return trackingIds;
  }

  @Override
  public CargoStatus loadCargoForTracking(String trackingIdValue) {
    TrackingId trackingId = new TrackingId(trackingIdValue);
    Cargo cargo = cargoRepository.findByTrackingId(trackingId).orElse(null);

    if (cargo == null) {
      return null;
    }

    List<HandlingEvent> handlingEvents =
        handlingEventRepository
            .lookupHandlingHistoryOfCargo(trackingId)
            .getDistinctEventsByCompletionTime();

    return cargoStatusDtoAssembler.toDto(cargo, handlingEvents);
  }

  @Override
  public List<RouteCandidate> requestPossibleRoutesForCargo(String trackingId) {
    List<Itinerary> itineraries =
        bookingService.requestPossibleRoutesForCargo(new TrackingId(trackingId));

    List<RouteCandidate> routeCandidates =
        itineraries
            .stream()
            .map(itineraryCandidateDtoAssembler::toDto)
            .collect(Collectors.toList());

    return routeCandidates;
  }
}
