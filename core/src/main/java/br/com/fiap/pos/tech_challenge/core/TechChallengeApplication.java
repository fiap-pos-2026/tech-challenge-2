package br.com.fiap.pos.tech_challenge.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TechChallengeApplication {

	static void main(String[] args) {
		SpringApplication.run(TechChallengeApplication.class, args);
	}

}
