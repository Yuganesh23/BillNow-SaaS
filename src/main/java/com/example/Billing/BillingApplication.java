package com.example.Billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.example.Billing",
		"auth",
		"shop",
		"product",
		"customer",
		"invoice",
		"payment"
})
public class BillingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingApplication.class, args);
	}
}