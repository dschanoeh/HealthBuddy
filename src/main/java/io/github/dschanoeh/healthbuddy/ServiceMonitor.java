package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.configuration.HealthBuddyConfiguration;
import io.github.dschanoeh.healthbuddy.configuration.ServiceConfig;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import io.github.dschanoeh.healthbuddy.notifications.pushover.PushoverInvalidTokensException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceMonitor {

    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);
    private static final long EVALUATION_SPREAD_MS = 100;

    private final HealthBuddyConfiguration healthBuddyConfiguration;
    private final ThreadPoolTaskScheduler scheduler;
    private final List<NotificationChannel> channels;

    @Autowired(required = false)
    private ReferenceEndpointEvaluator referenceEndpointEvaluator;

    @Autowired
    @Qualifier("userAgent")
    private String userAgent;


    @PostConstruct
    public void startMonitoring() throws PushoverInvalidTokensException {
        logger.log(Level.DEBUG, "Setting up evaluators");

        if(referenceEndpointEvaluator != null) {
            logger.log(Level.DEBUG, "Configuring evaluators with reference endpoint");
        }

        LocalDateTime firstExecution = LocalDateTime.now();
        firstExecution = firstExecution.plusNanos(EVALUATION_SPREAD_MS*1000*1000);
        for(ServiceConfig c : healthBuddyConfiguration.getServices()) {
            try {
                logger.log(Level.DEBUG, "Scheduling endpoint evaluator for service '{}'", c.getName());
                EndpointEvaluator evaluator = new EndpointEvaluator(c, healthBuddyConfiguration.getNetwork(), channels, userAgent);
                if(referenceEndpointEvaluator != null) {
                   evaluator.setReferenceEndpointEvaluator(referenceEndpointEvaluator);
                }
                scheduler.scheduleAtFixedRate(evaluator::evaluate, java.sql.Timestamp.valueOf(firstExecution), healthBuddyConfiguration.getUpdateInterval());
                firstExecution = firstExecution.plusNanos(EVALUATION_SPREAD_MS*1000*1000);
            } catch (MalformedURLException ex) {
                logger.log(Level.ERROR, "Could not set up evaluator - Malformed URL found", ex);
            }
        }
    }
}
