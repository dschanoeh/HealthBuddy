package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import io.github.dschanoeh.healthbuddy.notifications.teams.TeamsNotificationChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
public class ServiceMonitor{

    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);

    Config config;
    @Autowired
    ThreadPoolTaskScheduler scheduler;

    public ServiceMonitor(Config config) {
        logger.log(Level.INFO, "Service monitor  initialized");
        this.config = config;
    }

    @PostConstruct
    public void startMonitoring() {
        logger.log(Level.DEBUG, "Setting up evaluators");
        NotificationChannel channel = new TeamsNotificationChannel(config.getTeams());

        for(ServiceConfig c : config.getServices()) {
            EndpointEvaluator evaluator = new EndpointEvaluator(c, config.getNetwork(), channel);
            scheduler.scheduleAtFixedRate(evaluator::evaluate, config.getUpdateInterval());
        }
    }


}
