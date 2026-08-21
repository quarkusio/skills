package org.eclipse.cargotracker.infrastructure.logging;

import java.util.logging.Logger;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class LoggerProducer {

  @Bean
  @Scope("prototype")
  public Logger logger(InjectionPoint injectionPoint) {
    // Spring equivalent does not have access to the bean
    String loggerName = injectionPoint.getMember().getDeclaringClass().getName();

    return Logger.getLogger(loggerName);
  }

}
