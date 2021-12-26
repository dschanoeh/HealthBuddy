package io.github.dschanoeh.healthbuddy.notifications.pushover;

import de.svenkubiak.jpushover.JPushover;
import de.svenkubiak.jpushover.exceptions.JPushoverException;
import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PushoverNotificationChannel implements NotificationChannel {
    private static final Logger logger = LogManager.getLogger(PushoverNotificationChannel.class);

    private static final String OPEN_INCIDENT_TITLE_PATTERN = "New Incident: [%s] %s";
    private static final String OPEN_INCIDENT_NOT_REACHABLE_MESSAGE_PATTERN = "The service could not be reached";
    private static final String OPEN_INCIDENT_UNEXPECTED_RESPONSE_MESSAGE_PATTERN = "Unexpected response (HTTP %d)";
    private static final String CLOSED_INCIDENT_TITLE_PATTERN = "Incident Resolved: %s - %s";
    private static final String CLOSED_INCIDENT_MESSAGE_PATTERN = "Received a valid response again";

    private final PushoverConfiguration configuration;
    private final List<Recipient> recipients = new ArrayList<>();

    public PushoverNotificationChannel(PushoverConfiguration configuration) throws PushoverInvalidTokensException {
        this.configuration = configuration;

        for(RecipientConfiguration recipientConfiguration : configuration.getRecipients()) {
            Boolean tokensValid = validateTokens(configuration.getApplicationToken(), recipientConfiguration.getToken());
            if (Boolean.FALSE.equals(tokensValid)) {
                throw new PushoverInvalidTokensException();
            }

            Recipient r = new Recipient(recipientConfiguration);
            recipients.add(r);
        }
    }

    private static Boolean validateTokens(String applicationToken, String userToken) {
        try {
            return JPushover.messageAPI()
                    .withToken(applicationToken)
                    .withUser(userToken)
                    .validate();
        } catch (JPushoverException ex) {
            logger.log(Level.ERROR, "Received exception when validating Pushover tokens", ex);
        }
        return false;
    }

    @Override
    public void openIncident(Incident i) {
        String title = String.format(OPEN_INCIDENT_TITLE_PATTERN, i.getEnvironment(), i.getServiceName());
        String message = "";
        if (i.getType() == Incident.Type.NOT_REACHABLE) {
            message = String.format(OPEN_INCIDENT_NOT_REACHABLE_MESSAGE_PATTERN);
        } else if (i.getType() == Incident.Type.UNEXPECTED_RESPONSE) {
            message = String.format(OPEN_INCIDENT_UNEXPECTED_RESPONSE_MESSAGE_PATTERN, i.getHttpStatus());
        }
        final String finalMessage = message;
        recipients.stream()
                .filter(u -> u.shouldBeNotifiedAbout(i))
                .forEach(u -> u.sendMessage(configuration.getApplicationToken(), title, finalMessage));
    }

    @Override
    public void closeIncident(Incident i) {
        String title = String.format(CLOSED_INCIDENT_TITLE_PATTERN, i.getEnvironment(), i.getServiceName());
        String message = CLOSED_INCIDENT_MESSAGE_PATTERN;
        recipients.stream()
                .filter(u -> u.shouldBeNotifiedAbout(i))
                .forEach(u -> u.sendMessage(configuration.getApplicationToken(), title, message));
    }
}
