package org.eclipse.cargotracker.infrastructure.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.eclipse.cargotracker.domain.model.cargo.Itinerary;
import org.eclipse.cargotracker.domain.model.cargo.Leg;
import org.eclipse.cargotracker.domain.model.cargo.RouteSpecification;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.domain.service.RoutingService;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaLocationRepository;
import org.eclipse.cargotracker.infrastructure.persistence.jpa.JpaVoyageRepository;
import org.eclipse.pathfinder.api.TransitEdge;
import org.eclipse.pathfinder.api.TransitPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Our end of the routing service. This is basically a data model translation layer between our
 * domain model and the API put forward by the routing team, which operates in a different context
 * from us.
 */
@Service
@Transactional
public class ExternalRoutingService implements RoutingService {

  private Logger logger;

  @Value("${app.GraphTraversalUrl}")
  private String graphTraversalUrl;

  private RestClient restClient;

  private JpaLocationRepository locationRepository;
  private JpaVoyageRepository voyageRepository;

  public ExternalRoutingService(Logger logger,
      JpaLocationRepository locationRepository,
      JpaVoyageRepository voyageRepository) {
    this.logger = logger;
    this.locationRepository = locationRepository;
    this.voyageRepository = voyageRepository;
  }

  @PostConstruct
  public void init() {
    restClient = RestClient.create(graphTraversalUrl);
  }

  @Override
  public List<Itinerary> fetchRoutesForSpecification(RouteSpecification routeSpecification) {
    // The RouteSpecification is picked apart and adapted to the external API.
    String origin = routeSpecification.getOrigin().getUnLocode().getIdString();
    String destination = routeSpecification.getDestination().getUnLocode().getIdString();

    List<TransitPath> transitPaths =
        restClient.get()
            .uri(uriBuilder -> uriBuilder.queryParam("origin", origin)
                .queryParam("destination", destination).build())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .body(new ParameterizedTypeReference<List<TransitPath>>() {});

    // The returned result is then translated back into our domain model.
    List<Itinerary> itineraries = new ArrayList<>();

    // Use the specification to safe-guard against invalid itineraries
    transitPaths
        .stream()
        .map(this::toItinerary)
        .forEach(
            itinerary -> {
              if (routeSpecification.isSatisfiedBy(itinerary)) {
                itineraries.add(itinerary);
              } else {
                logger.log(
                    Level.FINE, "Received itinerary that did not satisfy the route specification");
              }
            });

    return itineraries;
  }

  private Itinerary toItinerary(TransitPath transitPath) {
    List<Leg> legs =
        transitPath.getTransitEdges().stream().map(this::toLeg).collect(Collectors.toList());
    return new Itinerary(legs);
  }

  private Leg toLeg(TransitEdge edge) {
    return new Leg(
        voyageRepository.findByVoyageNumber(new VoyageNumber(edge.getVoyageNumber())),
        locationRepository.findByUnLocode(new UnLocode(edge.getFromUnLocode())),
        locationRepository.findByUnLocode(new UnLocode(edge.getToUnLocode())),
        edge.getFromDate(),
        edge.getToDate());
  }
}
