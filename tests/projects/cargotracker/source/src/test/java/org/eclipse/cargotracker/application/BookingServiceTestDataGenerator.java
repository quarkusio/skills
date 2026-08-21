package org.eclipse.cargotracker.application;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Loads sample data for demo. */
@Component
@Profile("test")
public class BookingServiceTestDataGenerator {

  @Autowired
  private TestDataGenerator generator;

  @PostConstruct
  public void loadSampleData() {
    // can't have @Transactional for calls inside the same class
    generator.loadData();
  }

}
