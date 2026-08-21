package org.eclipse.cargotracker.interfaces.booking.facade.internal.assembler;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.RoutingStatus;
import org.eclipse.cargotracker.domain.model.cargo.TransportStatus;
import org.eclipse.cargotracker.interfaces.booking.facade.dto.CargoRoute;
import org.eclipse.cargotracker.interfaces.booking.facade.dto.Leg;
import org.springframework.stereotype.Component;

@Component
public class CargoRouteDtoAssembler {

  private LocationDtoAssembler locationDtoAssembler;

  public CargoRouteDtoAssembler(LocationDtoAssembler locationDtoAssembler) {
    this.locationDtoAssembler = locationDtoAssembler;
  }

  public CargoRoute toDto(Cargo cargo) {
    List<Leg> legs =
        cargo
            .getItinerary()
            .getLegs()
            .stream()
            .map(
                leg -> new Leg(
                    leg.getVoyage().getVoyageNumber().getIdString(),
                    locationDtoAssembler.toDto(leg.getLoadLocation()),
                    locationDtoAssembler.toDto(leg.getUnloadLocation()),
                    leg.getLoadTime(),
                    leg.getUnloadTime()))
            .collect(toList());

    return new CargoRoute(
        cargo.getTrackingId().getIdString(),
        locationDtoAssembler.toDto(cargo.getOrigin()),
        locationDtoAssembler.toDto(cargo.getRouteSpecification().getDestination()),
        cargo.getRouteSpecification().getArrivalDeadline(),
        cargo.getDelivery().getRoutingStatus().sameValueAs(RoutingStatus.MISROUTED),
        cargo.getDelivery().getTransportStatus().sameValueAs(TransportStatus.CLAIMED),
        locationDtoAssembler.toDto(cargo.getDelivery().getLastKnownLocation()),
        cargo.getDelivery().getTransportStatus().name(),
        legs);
  }
}
