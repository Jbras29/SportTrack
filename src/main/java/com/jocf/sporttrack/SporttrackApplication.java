package com.jocf.sporttrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SporttrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SporttrackApplication.class, args);
	}

}
