package br.com.brisabr.helpdesk_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"br.com.brisabr.helpdesk_api"})
public class HelpdeskApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpdeskApiApplication.class, args);
	}

	
}