package application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"api", "application", "domain", "infra"})
public class EventIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventIntelligenceApplication.class, args);
    }

}

