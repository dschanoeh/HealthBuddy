package io.github.dschanoeh.healthbuddy.notifications.teams;

import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.NetworkConfig;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TeamsNotificationChannel implements NotificationChannel {
    private static final Logger logger = LogManager.getLogger(TeamsNotificationChannel.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss z");

    TeamsConfiguration configuration;
    private final List<WebHook> hooks;

    public TeamsNotificationChannel(TeamsConfiguration configuration, NetworkConfig networkConfiguration) {
        logger.log(Level.INFO, "TeamsNotificationChannel created");
        this.configuration = configuration;

        hooks = new ArrayList<>();
        for (WebHookConfiguration c : configuration.getHooks()) {
            WebHook hook = new WebHook(c, networkConfiguration);
            hooks.add(hook);
        }
    }
    @Override
    public void openIncident(Incident i) {
        logger.log(Level.INFO, "Sending openIncident notification");
        TeamsMessage message = new TeamsMessage();
        message.setThemeColor(TeamsMessage.COLOR_RED);
        message.setTitle("New Incident");
        message.setSummary(String.format("A new incident for the service '%s' was opened", i.getServiceName()));
        TeamsMessageSection section = new TeamsMessageSection();
        message.getSections().add(section);
        List<TeamsMessageSection.Fact> facts = section.getFacts();
        switch(i.getType()) {
            case UNEXPECTED_RESPONSE:
                section.setActivityTitle("Unexpected response from observed endpoint");
                facts.add(new TeamsMessageSection.Fact("Service", i.getServiceName()));
                if(i.getEnvironment() != null) {
                    facts.add(new TeamsMessageSection.Fact("Environment", i.getEnvironment()));
                }
                if(i.getBody() != null) {
                    facts.add(new TeamsMessageSection.Fact("Response", i.getBody()));
                }
                if(i.getHttpStatus() != null) {
                    facts.add(new TeamsMessageSection.Fact("Status Code", String.valueOf(i.getHttpStatus())));
                }
                if(i.getUrl() != null) {
                    facts.add(new TeamsMessageSection.Fact("URL", i.getUrl()));
                }
                if(i.getStartDate() != null) {
                    facts.add(new TeamsMessageSection.Fact("Start Date", dateTimeFormatter.format(i.getStartDate())));
                }
                break;
            case NOT_REACHABLE:
                section.setActivityTitle("The observed endpoint is not reachable");
                facts.add(new TeamsMessageSection.Fact("Service", i.getServiceName()));
                if(i.getEnvironment() != null) {
                    facts.add(new TeamsMessageSection.Fact("Environment", i.getEnvironment()));
                }
                if(i.getUrl() != null) {
                    facts.add(new TeamsMessageSection.Fact("URL", i.getUrl()));
                }
                if(i.getStartDate() != null) {
                    facts.add(new TeamsMessageSection.Fact("Start Date", dateTimeFormatter.format(i.getStartDate())));
                }
                break;
            default:
                logger.log(Level.ERROR, "Received message of unknown type");
                return;
        }

        triggerHooks(i, message);
    }

    @Override
    public void closeIncident(Incident i) {
        logger.log(Level.INFO, "Sending closeIncident notification");
        TeamsMessage message = new TeamsMessage();
        message.setThemeColor(TeamsMessage.COLOR_GREEN);
        message.setTitle("Incident Resolved");
        message.setSummary(String.format("The incident for the service '%s' has been resolved", i.getServiceName()));
        TeamsMessageSection section = new TeamsMessageSection();
        message.getSections().add(section);
        List<TeamsMessageSection.Fact> facts = section.getFacts();
        facts.add(new TeamsMessageSection.Fact("Service", i.getServiceName()));
        if(i.getEnvironment() != null) {
            facts.add(new TeamsMessageSection.Fact("Environment", i.getEnvironment()));
        }
        if(i.getStartDate() != null) {
            facts.add(new TeamsMessageSection.Fact("Start Date", dateTimeFormatter.format(i.getStartDate())));
        }
        if(i.getEndDate() != null) {
            facts.add(new TeamsMessageSection.Fact("End Date", dateTimeFormatter.format(i.getEndDate())));
        }
        if(i.getEndDate() != null && i.getStartDate() != null) {
            Duration duration = Duration.between(i.getStartDate(), i.getEndDate());
            Long s = duration.getSeconds();
            String durationString = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            facts.add(new TeamsMessageSection.Fact("Duration", durationString));
        }
        triggerHooks(i, message);
    }

    private void triggerHooks(Incident i, TeamsMessage m) {
        for (WebHook hook : this.hooks) {
            if (hook.isResponsible(i)) {
                hook.send(m);
            }
        }
    }
}
