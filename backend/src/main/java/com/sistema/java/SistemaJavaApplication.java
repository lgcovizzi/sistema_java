package com.sistema.java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SistemaJavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaJavaApplication.class, args);
	}

}