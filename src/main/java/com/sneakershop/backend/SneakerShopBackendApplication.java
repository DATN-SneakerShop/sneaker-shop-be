package com.sneakershop.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SneakerShopBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SneakerShopBackendApplication.class, args);
	}

}
