package io.github.dschanoeh.healthbuddy.notifications.pushover;

import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecipientTest {
    private static final String PROD_PATTERN = "^prod$";

    @Mock
    private NotificationChannel channel;
    private AutoCloseable closeable;

    @BeforeEach
    public void openMocks() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void releaseMocks() throws Exception {
        closeable.close();
    }

    @Test
    void recipientEnvironmentPatternMatchesTest() {
        RecipientConfiguration recipientConfiguration = new RecipientConfiguration();
        recipientConfiguration.setEnvironmentPattern(PROD_PATTERN);
        Recipient r = new Recipient(recipientConfiguration);


        Incident i = new Incident(Incident.Type.UNEXPECTED_RESPONSE, List.of(channel));
        i.setEnvironment("prod");
        assertTrue(r.shouldBeNotifiedAbout(i));
    }

    @Test
    void recipientEnvironmentPatternNoMatchTest() {
        RecipientConfiguration recipientConfiguration = new RecipientConfiguration();
        recipientConfiguration.setEnvironmentPattern(PROD_PATTERN);
        Recipient r = new Recipient(recipientConfiguration);


        Incident i = new Incident(Incident.Type.UNEXPECTED_RESPONSE, List.of(channel));
        i.setEnvironment("proda");
        assertFalse(r.shouldBeNotifiedAbout(i));

        i.setEnvironment("test");
        assertFalse(r.shouldBeNotifiedAbout(i));
    }
}
