package com.avanzapp.avanzapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class AvanzappApplication {

	public static void main(String[] args) {
		SpringApplication.run(AvanzappApplication.class, args);
		System.out.print("El servidor ON");
		System.out.println("Listo para GITFLOW");
	}
}
