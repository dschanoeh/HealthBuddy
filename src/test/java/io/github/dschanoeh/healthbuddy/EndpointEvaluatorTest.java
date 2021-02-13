package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyMode;
import io.specto.hoverfly.junit.dsl.ResponseCreators;
import io.specto.hoverfly.junit5.HoverflyExtension;
import io.specto.hoverfly.junit5.api.HoverflyConfig;
import io.specto.hoverfly.junit5.api.HoverflyCore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Arrays;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static org.mockito.Mockito.*;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;

@ExtendWith(HoverflyExtension.class)
@HoverflyCore(mode = HoverflyMode.SIMULATE, config = @HoverflyConfig(proxyPort = 8080))
public class EndpointEvaluatorTest {
    protected static final Integer HOVERFLY_PORT = 8080;
    private static final String INVALID_URL = "http://127.0.0.1:1234";
    private static final String SAMPLE_HEALTHY_SERVICE = "http://www.my-service.com";
    private static final String SAMPLE_HEALTH_PATH = "/health";
    private static final String NO_CONTENT_PATH = "/noContent";
    private static final String SAMPLE_DEGRADED_SERVICE = "http://www.my-degraded-service.com";
    private static final String SAMPLE_SERVICE_NAME = "Foo service";

    private NotificationChannel channel = mock(NotificationChannel.class);
    private NetworkConfig networkConfig = new NetworkConfig();

    public EndpointEvaluatorTest() {
        networkConfig.setHttpProxyHost("127.0.0.1");
        networkConfig.setHttpProxyPort(HOVERFLY_PORT);
    }

    @Test
    public void noConnection() {
        ServiceConfig config = new ServiceConfig();
        config.setUrl(INVALID_URL);
        config.setName(SAMPLE_SERVICE_NAME);
        NetworkConfig customNetworkConf = new NetworkConfig();
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConf, channel);
        evaluator.evaluate();
        verify(channel).openIncident(any());
    }

    @Test
    public void successfulResponse(Hoverfly hoverfly) {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE +SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }

    @Test
    public void negativeResponse(Hoverfly hoverfly) {
        hoverfly.simulate(dsl(service(SAMPLE_DEGRADED_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(serverError())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_DEGRADED_SERVICE+SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    public void differentStatusCode(Hoverfly hoverfly) {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(NO_CONTENT_PATH).willReturn(ResponseCreators.noContent())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE+NO_CONTENT_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }
}
