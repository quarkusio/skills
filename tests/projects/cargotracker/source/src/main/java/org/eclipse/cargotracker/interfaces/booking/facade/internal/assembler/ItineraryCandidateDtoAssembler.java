package org.eclipse.cargotracker.interfaces.booking.facade.internal.assembler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.cargotracker.application.util.DateConverter;
import org.eclipse.cargotracker.domain.model.cargo.Itinerary;
import org.eclipse.cargotracker.domain.model.cargo.Leg;
import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaLocationRepository;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaVoyageRepository;
import org.eclipse.cargotracker.interfaces.booking.facade.dto.RouteCandidate;
import org.springframework.stereotype.Component;

@Component
public class ItineraryCandidateDtoAssembler {

  private LocationDtoAssembler locationDtoAssembler;

  public ItineraryCandidateDtoAssembler(LocationDtoAssembler locationDtoAssembler) {
    this.locationDtoAssembler = locationDtoAssembler;
  }

  public RouteCandidate toDto(Itinerary itinerary) {
    List<org.eclipse.cargotracker.interfaces.booking.facade.dto.Leg> legDTOs =
        itinerary.getLegs().stream().map(this::toLegDTO).collect(Collectors.toList());
    return new RouteCandidate(legDTOs);
  }

  protected org.eclipse.cargotracker.interfaces.booking.facade.dto.Leg toLegDTO(Leg leg) {
    VoyageNumber voyageNumber = leg.getVoyage().getVoyageNumber();
    return new org.eclipse.cargotracker.interfaces.booking.facade.dto.Leg(
        voyageNumber.getIdString(),
        locationDtoAssembler.toDto(leg.getLoadLocation()),
        locationDtoAssembler.toDto(leg.getUnloadLocation()),
        leg.getLoadTime(),
        leg.getUnloadTime());
  }

  public Itinerary fromDTO(
      RouteCandidate routeCandidateDTO,
      JpaVoyageRepository voyageRepository,
      JpaLocationRepository locationRepository) {
    List<Leg> legs = new ArrayList<>(routeCandidateDTO.getLegs().size());

    for (org.eclipse.cargotracker.interfaces.booking.facade.dto.Leg legDTO : routeCandidateDTO
        .getLegs()) {
      VoyageNumber voyageNumber = new VoyageNumber(legDTO.getVoyageNumber());
      Voyage voyage = voyageRepository.findByVoyageNumber(voyageNumber);
      Location from = locationRepository.findByUnLocode(new UnLocode(legDTO.getFromUnLocode()));
      Location to = locationRepository.findByUnLocode(new UnLocode(legDTO.getToUnLocode()));

      legs.add(
          new Leg(
              voyage,
              from,
              to,
              DateConverter.toDateTime(legDTO.getLoadTime()),
              DateConverter.toDateTime(legDTO.getUnloadTime())));
    }

    return new Itinerary(legs);
  }
}
