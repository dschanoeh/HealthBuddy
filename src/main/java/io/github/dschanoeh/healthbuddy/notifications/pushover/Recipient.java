package io.github.dschanoeh.healthbuddy.notifications.pushover;

import de.svenkubiak.jpushover.JPushover;
import de.svenkubiak.jpushover.exceptions.JPushoverException;
import de.svenkubiak.jpushover.http.PushoverResponse;
import io.github.dschanoeh.healthbuddy.notifications.AbstractNotificationReceiver;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Recipient extends AbstractNotificationReceiver {
    private static final Logger logger = LogManager.getLogger(Recipient.class);

    private final RecipientConfiguration configuration;

    public Recipient(RecipientConfiguration config) {
        this.configuration = config;
        this.setEnvironmentPattern(configuration.getCompiledEnvironmentPattern());
    }

    public void sendMessage(String applicationToken, String title, String message) {
        try {
            PushoverResponse pushoverResponse = JPushover.messageAPI()
                    .withToken(applicationToken)
                    .withUser(configuration.getToken())
                    .withTitle(title)
                    .withMessage(message)
                    .withPriority(configuration.getPriority())
                    .push();
            if (!pushoverResponse.isSuccessful()) {
                logger.log(Level.ERROR, "Was not able to send pushover message: {}", pushoverResponse.getResponse());
            }
        } catch (JPushoverException ex) {
            logger.log(Level.ERROR, "Received exception when attempting to send pushover message", ex);
        }
    }
}
