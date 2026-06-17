package com.microservicio.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class MicroservicioConfiguracionesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioConfiguracionesApplication.class, args);
	}

}
