package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.configuration.HealthBuddyConfiguration;
import io.github.dschanoeh.healthbuddy.configuration.ServiceConfig;
import io.github.dschanoeh.healthbuddy.dto.IncidentDTO;
import io.github.dschanoeh.healthbuddy.dto.ServiceStatusDTO;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class ServiceMonitor {

    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);
    private static final long EVALUATION_SPREAD_MS = 100;

    private final HealthBuddyConfiguration healthBuddyConfiguration;
    private final ThreadPoolTaskScheduler scheduler;
    @Autowired
    @Qualifier("allNotificationChannels")
    private List<NotificationChannel> channels;

    @Autowired(required = false)
    private ReferenceEndpointEvaluator referenceEndpointEvaluator;

    @Autowired
    @Qualifier("userAgent")
    private String userAgent;

    private final Map<UUID, EndpointEvaluator> evaluators = new HashMap<>();


    @PostConstruct
    public void startMonitoring() {
        logger.log(Level.DEBUG, "Setting up evaluators");

        if(referenceEndpointEvaluator != null) {
            logger.log(Level.DEBUG, "Configuring evaluators with reference endpoint");
        }

        LocalDateTime firstExecution = LocalDateTime.now();
        firstExecution = firstExecution.plusNanos(EVALUATION_SPREAD_MS*1000*1000);
        for(ServiceConfig c : healthBuddyConfiguration.getServices()) {
            try {
                logger.log(Level.DEBUG, "Scheduling endpoint evaluator for service '{}'", c.getName());
                EndpointEvaluator evaluator = new EndpointEvaluator(c, healthBuddyConfiguration.getNetwork(), healthBuddyConfiguration.getDashboard(), channels, userAgent);
                evaluators.put(c.getId(),evaluator);
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

    public List<ServiceStatusDTO> getServiceStatus() {
        List<ServiceStatusDTO> list = new ArrayList<>();
        for(ServiceConfig service : healthBuddyConfiguration.getServices()) {
            ServiceStatusDTO statusDTO = getServiceStatus(service.getId());
            list.add(statusDTO);
        }
        return list;
    }

    public ServiceStatusDTO getServiceStatus(UUID id) {
        List<ServiceConfig> services = healthBuddyConfiguration.getServices();
        Optional<ServiceConfig> serviceOptional = services.stream().filter(s -> s.getId().equals(id)).findFirst();
        if(serviceOptional.isPresent()) {
            ServiceConfig service = serviceOptional.get();
            ServiceStatusDTO statusDTO = ServiceStatusDTO.builder()
                    .id(service.getId())
                    .environment(service.getEnvironment())
                    .name(service.getName())
                    .url(service.getUrl())
                    .build();
            EndpointEvaluator evaluator = evaluators.get(service.getId());
            if (evaluator != null) {
                if(Boolean.TRUE.equals(evaluator.isUp())) {
                    statusDTO.setIsUp(Boolean.TRUE);
                } else {
                    statusDTO.setIsUp(Boolean.FALSE);
                    Incident i = evaluator.getCurrentIncident();
                    IncidentDTO incidentDTO = new IncidentDTO();
                    incidentDTO.setType(i.getType());
                    incidentDTO.setStartDate(i.getStartDate());
                    incidentDTO.setEndDate(i.getEndDate());
                    incidentDTO.setBody(i.getBody());
                    incidentDTO.setStatusCode(i.getHttpStatus());
                    statusDTO.setCurrentIncident(incidentDTO);
                }
            }
            return statusDTO;
        }
        logger.log(Level.INFO, "Could not find service config for a service with id {}", id);
        return null;
    }
}
