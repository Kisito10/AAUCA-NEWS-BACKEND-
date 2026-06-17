package com.microservicio.secciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class MicroservicioSeccionesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioSeccionesApplication.class, args);
	}

}
