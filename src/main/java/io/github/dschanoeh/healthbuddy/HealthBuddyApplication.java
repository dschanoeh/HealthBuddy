package io.github.dschanoeh.healthbuddy;

import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Generated
public class HealthBuddyApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(HealthBuddyApplication.class);
        app.run();
    }
}