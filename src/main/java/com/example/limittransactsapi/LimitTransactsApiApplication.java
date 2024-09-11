package com.example.limittransactsapi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Включаем поддержку планирования задач
public class LimitTransactsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LimitTransactsApiApplication.class, args);
	}

}
