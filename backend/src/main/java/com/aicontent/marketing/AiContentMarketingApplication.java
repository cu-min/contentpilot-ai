package com.aicontent.marketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiContentMarketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiContentMarketingApplication.class, args);
    }
}
