package com.example.limittransactsapi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // Импортируйте эту аннотацию

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.example.limittransactsapi.repository") // Добавьте эту строку
public class LimitTransactsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LimitTransactsApiApplication.class, args);
	}
}
