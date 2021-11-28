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

    private final PushoverConfiguration configuration;
    private final List<Recipient> recipients = new ArrayList<>();

    public PushoverNotificationChannel(PushoverConfiguration configuration) throws PushoverInvalidTokensException {
        this.configuration = configuration;

        for(RecipientConfiguration recipientConfiguration : configuration.getRecipients()) {
            Boolean tokensValid = validateTokens(configuration.getApplicationToken(), recipientConfiguration.getToken());
            if (!tokensValid) {
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
        String title = String.format("New Incident: [%s] %s", i.getEnvironment(), i.getServiceName());
        String message = String.format("Affected service: %s - %s", i.getEnvironment(), i.getServiceName());
        recipients.stream()
                .filter(u -> u.shouldBeNotifiedAbout(i))
                .forEach(u -> u.sendMessage(configuration.getApplicationToken(), title, message));
    }

    @Override
    public void closeIncident(Incident i) {
        String title = String.format("Incident Resolved: %s - %s", i.getEnvironment(), i.getServiceName());
        String message = "";
        recipients.stream()
                .filter(u -> u.shouldBeNotifiedAbout(i))
                .forEach(u -> u.sendMessage(configuration.getApplicationToken(), title, message));
    }
}
