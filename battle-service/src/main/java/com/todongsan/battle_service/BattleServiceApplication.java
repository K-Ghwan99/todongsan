package com.todongsan.battle_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BattleServiceApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory(System.getProperty("user.dir"))
				.ignoreIfMissing()
				.load();

		System.setProperty("DB_HOST", dotenv.get("DB_HOST", ""));
		System.setProperty("DB_PORT", dotenv.get("DB_PORT", ""));
		System.setProperty("DB_NAME", dotenv.get("DB_NAME", ""));
		System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME", ""));
		System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD", ""));
		System.setProperty("MEMBER_POINT_SERVICE_URL", dotenv.get("MEMBER_POINT_SERVICE_URL", ""));
		System.setProperty("INSIGHT_SERVICE_URL", dotenv.get("INSIGHT_SERVICE_URL", ""));
		System.setProperty("INTERNAL_AUTH_TOKEN", dotenv.get("INTERNAL_AUTH_TOKEN", ""));

		SpringApplication.run(BattleServiceApplication.class, args);
	}

}