package com.microservicio.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MicroservicioUsersApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioUsersApplication.class, args);
	}

}
