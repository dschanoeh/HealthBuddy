package io.github.dschanoeh.healthbuddy.notifications.pushover;

import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipientTest {
    private static final String PROD_PATTERN = "^prod$";

    @Mock
    private NotificationChannel channel;
    private AutoCloseable closeable;
    private final UUID serviceID = UUID.randomUUID();

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

        Incident i = new Incident(Incident.Type.UNEXPECTED_RESPONSE, serviceID, List.of(channel));
        i.setEnvironment("prod");
        assertTrue(r.shouldBeNotifiedAbout(i));

        Incident genericIncident = new Incident(Incident.Type.UNEXPECTED_RESPONSE, serviceID, List.of(channel));
        assertFalse(r.shouldBeNotifiedAbout(genericIncident));
    }

    @Test
    void recipientEnvironmentPatternNoMatchTest() {
        RecipientConfiguration recipientConfiguration = new RecipientConfiguration();
        recipientConfiguration.setEnvironmentPattern(PROD_PATTERN);
        Recipient r = new Recipient(recipientConfiguration);

        Incident i = new Incident(Incident.Type.UNEXPECTED_RESPONSE, serviceID, List.of(channel));
        i.setEnvironment("proda");
        assertFalse(r.shouldBeNotifiedAbout(i));

        i.setEnvironment("test");
        assertFalse(r.shouldBeNotifiedAbout(i));
    }

    @Test
    void recipientWithoutEnvironmentTest() {
        RecipientConfiguration recipientConfiguration = new RecipientConfiguration();
        Recipient r = new Recipient(recipientConfiguration);

        Incident i = new Incident(Incident.Type.UNEXPECTED_RESPONSE, serviceID, List.of(channel));
        i.setEnvironment("proda");
        assertTrue(r.shouldBeNotifiedAbout(i));

        i.setEnvironment("test");
        assertTrue(r.shouldBeNotifiedAbout(i));
    }
}
