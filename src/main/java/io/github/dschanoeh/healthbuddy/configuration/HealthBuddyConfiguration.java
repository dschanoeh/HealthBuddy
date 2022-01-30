package io.github.dschanoeh.healthbuddy.configuration;

import io.github.dschanoeh.healthbuddy.ReferenceEndpointEvaluator;
import io.github.dschanoeh.healthbuddy.notifications.IncidentHistoryCollector;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import io.github.dschanoeh.healthbuddy.notifications.NotificationServiceConfiguration;
import io.github.dschanoeh.healthbuddy.notifications.pushover.PushoverInvalidTokensException;
import io.github.dschanoeh.healthbuddy.notifications.pushover.PushoverNotificationChannel;
import io.github.dschanoeh.healthbuddy.notifications.teams.TeamsNotificationChannel;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.info.BuildProperties;
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
    private static final Logger logger = LogManager.getLogger(HealthBuddyConfiguration.class);
    private static final String USER_AGENT_PREFIX = "HealthBuddy ";
    private static final Integer THREAD_POOL_SIZE = 4;

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
    @Getter
    @Setter
    @NestedConfigurationProperty
    private ReferenceEndpointConfiguration referenceEndpoint;
    @Getter
    @Setter
    @NestedConfigurationProperty
    private DashboardConfiguration dashboard = new DashboardConfiguration();

    @Bean(name = "evaluatorThreadPool")
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(THREAD_POOL_SIZE);
        threadPoolTaskScheduler.setThreadNamePrefix("Evaluator");
        return threadPoolTaskScheduler;
    }

    @Bean
    public NetworkConfig networkConfiguration() {
        return network;
    }

    @Bean
    public ReferenceEndpointEvaluator referenceEndpointEvaluator() {
        if(referenceEndpoint != null) {
            return new ReferenceEndpointEvaluator(referenceEndpoint.getUrl());
        }
        return null;
    }

    @Bean(name = "allNotificationChannels")
    public List<NotificationChannel> notificationChannels(IncidentHistoryCollector incidentHistoryCollector) {
        ArrayList<NotificationChannel> channels = new ArrayList<>();
        if(this.getNotificationServices().getTeams() != null) {
            NotificationChannel teamsNotificationChannel = new TeamsNotificationChannel(this.getNotificationServices().getTeams(), this.getNetwork());
            channels.add(teamsNotificationChannel);
        }
        if(this.getNotificationServices().getPushover() != null) {
            try {
                NotificationChannel pushoverNotificationChannel = new PushoverNotificationChannel(this.getNotificationServices().getPushover());
                channels.add(pushoverNotificationChannel);
            } catch (PushoverInvalidTokensException ex) {
                logger.log(Level.ERROR, "Could not set up pushover notification channel", ex);
            }
        }
        channels.add(incidentHistoryCollector);
        return channels;
    }


    @Bean(name = "userAgent")
    public String userAgent(BuildProperties buildProperties) {
       return USER_AGENT_PREFIX + buildProperties.getVersion();
    }

}
