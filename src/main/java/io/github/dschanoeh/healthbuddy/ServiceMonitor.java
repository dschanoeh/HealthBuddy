package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import io.github.dschanoeh.healthbuddy.notifications.teams.TeamsNotificationChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.time.LocalDateTime;

@Component
public class ServiceMonitor{

    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);
    private static final long EVALUATION_SPREAD_MS = 100;
    private static final String USER_AGENT_PREFIX = "HealthBuddy ";

    Config config;
    @Autowired
    ThreadPoolTaskScheduler scheduler;
    @Autowired
    private BuildProperties buildProperties;

    public ServiceMonitor(Config config) {
        logger.log(Level.INFO, "Service monitor  initialized");
        this.config = config;
    }

    @PostConstruct
    public void startMonitoring() {
        String userAgent = USER_AGENT_PREFIX + buildProperties.getVersion();

        logger.log(Level.DEBUG, "Setting up evaluators");
        NotificationChannel channel = new TeamsNotificationChannel(config.getTeams(), config.getNetwork());
        LocalDateTime firstExecution = LocalDateTime.now();
        firstExecution = firstExecution.plusNanos(EVALUATION_SPREAD_MS*1000*1000);
        for(ServiceConfig c : config.getServices()) {
            try {
                logger.log(Level.DEBUG, "Scheduling endpoint evaluator for service '{}'", c.getName());
                EndpointEvaluator evaluator = new EndpointEvaluator(c, config.getNetwork(), channel, userAgent);
                scheduler.scheduleAtFixedRate(evaluator::evaluate, java.sql.Timestamp.valueOf(firstExecution), config.getUpdateInterval());
                firstExecution = firstExecution.plusNanos(EVALUATION_SPREAD_MS*1000*1000);
            } catch (MalformedURLException ex) {
                logger.log(Level.ERROR, "Could not set up evaluator - Malformed URL found", ex);
            }
        }
    }


}
