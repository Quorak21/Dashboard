package com.dokkcorp.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableScheduling
public class DashboardApplication {

	@Bean
	// Création du restclient global pour toute l'application

	public static RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

	// Et on lance le backend !
	public static void main(String[] args) {
		SpringApplication.run(DashboardApplication.class, args);
	}

}
