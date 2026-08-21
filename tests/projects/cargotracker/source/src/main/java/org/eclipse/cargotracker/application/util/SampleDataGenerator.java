package org.eclipse.cargotracker.application.util;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Loads sample data for demo. */
@Component
@Profile("!test") // hacky way of disabling the component for tests
public class SampleDataGenerator {

  private InitialLoader loader;

  public SampleDataGenerator(InitialLoader loader) {
    this.loader = loader;
  }

  @PostConstruct
  public void loadSampleData() {
    // @Transactional works on proxy-based AOP
    // when you call the method from inside the class, it's bypassed
    loader.loadData();
  }
}
