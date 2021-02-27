package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyMode;
import io.specto.hoverfly.junit.dsl.ResponseCreators;
import io.specto.hoverfly.junit5.HoverflyExtension;
import io.specto.hoverfly.junit5.api.HoverflyConfig;
import io.specto.hoverfly.junit5.api.HoverflyCore;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.HttpBodyConverter.jsonWithSingleQuotes;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.mockito.Mockito.*;

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
    private static final String SAMPLE_BASIC_AUTH_USER = "foo";
    private static final String SAMPLE_BASIC_AUTH_PASS = "bar";
    private static final String SAMPLE_BASIC_AUTH_BASE64 = new String(Base64.encodeBase64(
            (SAMPLE_BASIC_AUTH_USER + ":" + SAMPLE_BASIC_AUTH_PASS).getBytes(StandardCharsets.ISO_8859_1)
    ));

    private final NotificationChannel channel = mock(NotificationChannel.class);
    private final NetworkConfig networkConfig = new NetworkConfig();

    public EndpointEvaluatorTest() {
        networkConfig.setHttpProxyHost("127.0.0.1");
        networkConfig.setHttpProxyPort(HOVERFLY_PORT);
    }

    @Test
    public void noConnection() throws URISyntaxException {
        ServiceConfig config = new ServiceConfig();
        config.setUrl(INVALID_URL);
        config.setName(SAMPLE_SERVICE_NAME);
        NetworkConfig customNetworkConf = new NetworkConfig();
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConf, channel);
        evaluator.evaluate();
        verify(channel).openIncident(any());
    }

    @Test
    public void successfulResponse(Hoverfly hoverfly) throws URISyntaxException {
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
    public void negativeResponse(Hoverfly hoverfly) throws URISyntaxException {
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
    public void differentStatusCode(Hoverfly hoverfly) throws URISyntaxException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(NO_CONTENT_PATH).willReturn(ResponseCreators.noContent())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE+NO_CONTENT_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    public void basicAuthTest(Hoverfly hoverfly) throws URISyntaxException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(ResponseCreators.success())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setUserName(SAMPLE_BASIC_AUTH_USER);
        config.setPassword(SAMPLE_BASIC_AUTH_PASS);
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        hoverfly.verify(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).anyBody().header("Authorization", "Basic " + SAMPLE_BASIC_AUTH_BASE64));
        verify(channel, never()).openIncident(any());
    }

    @Test
    public void validBodyTest(Hoverfly hoverfly) throws URISyntaxException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success().body(
                jsonWithSingleQuotes("{'status':'UP'}")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE +SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }

    @Test
    public void invalidBodyTest(Hoverfly hoverfly) throws URISyntaxException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success().body(
                jsonWithSingleQuotes("{'status':'UNKNOWN','details':{'foo':'bar'}}")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE +SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    public void emptyBodyTest(Hoverfly hoverfly) throws URISyntaxException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success().body(
                jsonWithSingleQuotes("")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE +SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    public void noStatusCodeTest(Hoverfly hoverfly) throws URISyntaxException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(serverError().body(
                jsonWithSingleQuotes("{'status':'UP'}")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE +SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, channel);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }
}
