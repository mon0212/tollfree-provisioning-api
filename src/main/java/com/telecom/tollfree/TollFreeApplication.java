package com.telecom.tollfree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TollFreeApplication {
	public static void main(String[] args) {
		SpringApplication.run(TollFreeApplication.class, args);
	}
}
