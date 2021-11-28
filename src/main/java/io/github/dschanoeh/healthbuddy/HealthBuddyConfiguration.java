package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationServiceConfiguration;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@EnableScheduling
@Validated
@Generated
public class HealthBuddyConfiguration {
    @NestedConfigurationProperty
    @Getter
    @Setter
    private List<ServiceConfig> services = new ArrayList<>();
    @Getter
    @Setter
    @NestedConfigurationProperty
    @NotNull(message = "Notification services must be configured")
    private NotificationServiceConfiguration notificationServices;
    @NestedConfigurationProperty
    @Getter
    @Setter
    private NetworkConfig network = new NetworkConfig();
    @Getter
    @Setter
    @NotNull(message = "Update Interval must be specified")
    private Integer updateInterval;

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(services.size());
        threadPoolTaskScheduler.setThreadNamePrefix("Evaluator");
        return threadPoolTaskScheduler;
    }

}
