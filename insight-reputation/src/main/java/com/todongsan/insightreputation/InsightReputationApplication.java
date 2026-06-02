package com.todongsan.insightreputation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InsightReputationApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsightReputationApplication.class, args);
    }
}