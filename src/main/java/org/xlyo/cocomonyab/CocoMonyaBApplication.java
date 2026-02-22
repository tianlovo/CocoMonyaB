package org.xlyo.cocomonyab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CocoMonyaBApplication {

	public static void main(String[] args) {
		SpringApplication.run(CocoMonyaBApplication.class, args);
	}

}
