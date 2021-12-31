package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class IncidentTest {

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
    void openIncidentTest() {
        Incident i = new Incident(Incident.Type.UNEXPECTED_RESPONSE, serviceID, List.of(channel));

        i.open();
        assertEquals(Incident.Type.UNEXPECTED_RESPONSE, i.getType());
        assertNotNull(i.getStartDate());
        assertTrue(i.isOpen());
        assertNull(i.getEndDate());
        verify(channel).openIncident(i);
    }

    @Test
    void closeIncidentTest() {
        Incident i = new Incident(Incident.Type.UNEXPECTED_RESPONSE, serviceID, List.of(channel));
        i.open();

        i.close();
        assertNotNull(i.getStartDate());
        assertNotNull(i.getEndDate());
        assertFalse(i.isOpen());

        verify(channel).closeIncident(i);
    }
}
