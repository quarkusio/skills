package org.eclipse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableJpaRepositories
@EnableScheduling
@EnableTransactionManagement
public class CargoTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CargoTrackerApplication.class, args);
	}

}
