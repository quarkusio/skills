package com.ibm.websphere.samples.daytrader.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expose an additional HTTP connector on 9080 while HTTPS 9443 remains the main
 * server.port.
 * Mirrors Liberty's httpPort=9080, httpsPort=9443 setup.
 */
@Configuration
public class DualPortConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> dualPortCustomizer() {
        return factory -> {
            Connector http = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            http.setPort(9080);
            factory.addAdditionalTomcatConnectors(http);
        };
    }
}
