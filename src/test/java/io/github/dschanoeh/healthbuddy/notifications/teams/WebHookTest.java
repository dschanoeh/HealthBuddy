package io.github.dschanoeh.healthbuddy.notifications.teams;

import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.NetworkConfig;
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
        assertTrue(hook.shouldBeNotifiedAbout(i));
    }

    @Test
    void hookPatternMatchingNegativeTest() {
        Incident i = new Incident(Incident.Type.NOT_REACHABLE, null);
        i.setEnvironment("foo");
        assertFalse(hook.shouldBeNotifiedAbout(i));
    }

}