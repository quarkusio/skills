package com.ibm.websphere.samples.daytrader;

import com.ibm.websphere.samples.daytrader.support.ApplicationProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@SpringBootApplication
@EnableConfigurationProperties(ApplicationProps.class)
@ServletComponentScan // Enable support for @WebServlet, @WebFilter, etc.
public class DaytraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaytraderApplication.class, args);
    }
}
