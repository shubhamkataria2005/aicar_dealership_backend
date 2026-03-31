package com.Shubham.carDealership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CarDealershipApplication {
	public static void main(String[] args) {
		SpringApplication.run(CarDealershipApplication.class, args);
		System.out.println("🚗 Car Dealership Backend Started on http://localhost:5000");
	}
}