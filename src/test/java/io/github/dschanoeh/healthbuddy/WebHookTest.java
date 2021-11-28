package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.teams.WebHook;
import io.github.dschanoeh.healthbuddy.notifications.teams.WebHookConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class WebHookTest {

    private final NetworkConfig networkConfiguration = new NetworkConfig();
    WebHook hook;

    public WebHookTest() {
        WebHookConfiguration c = new WebHookConfiguration();
        c.setUrl("http://127.0.0.1:8080");
        c.setEnvironmentPattern("^test$");

        hook = new WebHook(c, networkConfiguration);
    }

    @Test
    void hookPatternMatchingPositiveTest() {
        Incident i = new Incident(Incident.Type.NOT_REACHABLE, null);
        i.setEnvironment("test");
        assertTrue(hook.isResponsible(i));
    }

    @Test
    void hookPatternMatchingNegativeTest() {
        Incident i = new Incident(Incident.Type.NOT_REACHABLE, null);
        i.setEnvironment("foo");
        assertFalse(hook.isResponsible(i));
    }

}
