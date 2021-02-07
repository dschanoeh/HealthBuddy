package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.teams.TeamsConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@EnableScheduling
@Validated
public class Config {
    @NestedConfigurationProperty
    private final List<ServiceConfig> services = new ArrayList<>();
    @NestedConfigurationProperty
    private TeamsConfiguration teams;
    @NotNull(message = "Update Interval must be specified")
    private Integer updateInterval;

    public List<ServiceConfig> getServices() {
        return services;
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(50);
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }

    public TeamsConfiguration getTeams() {
        return teams;
    }

    public void setTeams(TeamsConfiguration teams) {
        this.teams = teams;
    }

    public Integer getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(Integer updateInterval) {
        this.updateInterval = updateInterval;
    }
}
