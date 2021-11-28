package io.github.dschanoeh.healthbuddy.notifications.pushover;

import de.svenkubiak.jpushover.JPushover;
import de.svenkubiak.jpushover.exceptions.JPushoverException;
import io.github.dschanoeh.healthbuddy.Incident;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Recipient {
    private static final Logger logger = LogManager.getLogger(Recipient.class);

    private final RecipientConfiguration configuration;
    private final Pattern environmentPattern;

    public Recipient(RecipientConfiguration config) {
        this.configuration = config;
        this.environmentPattern = configuration.getCompiledEnvironmentPattern();
    }

    public Boolean shouldBeNotifiedAbout(Incident i) {
        if (environmentPattern != null && i.getEnvironment() != null) {
            Matcher matcher = environmentPattern.matcher(i.getEnvironment());
            return matcher.matches();
        }
        return true;
    }

    public void sendMessage(String applicationToken, String title, String message) {
        try {
            JPushover.messageAPI()
                    .withToken(applicationToken)
                    .withUser(configuration.getToken())
                    .withTitle(title)
                    .withMessage(message)
                    .withPriority(configuration.getPriority())
                    .push();
        } catch (JPushoverException ex) {
            logger.log(Level.ERROR, "Received exception when attempting to send pushover message", ex);
        }
    }
}
