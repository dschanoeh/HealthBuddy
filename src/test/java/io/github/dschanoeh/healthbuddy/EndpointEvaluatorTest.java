package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.configuration.DashboardConfiguration;
import io.github.dschanoeh.healthbuddy.configuration.NetworkConfig;
import io.github.dschanoeh.healthbuddy.configuration.ServiceConfig;
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
import org.mockito.InOrder;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.HttpBodyConverter.jsonWithSingleQuotes;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(HoverflyExtension.class)
@HoverflyCore(mode = HoverflyMode.SIMULATE, config = @HoverflyConfig(proxyPort = 8080))
class EndpointEvaluatorTest {
    protected static final Integer HOVERFLY_PORT = 8080;
    private static final String INVALID_URL = "http://127.0.0.1:1234";
    private static final String SAMPLE_HEALTHY_SERVICE = "http://www.my-service.com";
    private static final String SAMPLE_HEALTH_PATH = "/health";
    private static final String SAMPLE_SUCCESS_PATH = "/success";
    private static final String NO_CONTENT_PATH = "/noContent";
    private static final String SAMPLE_DEGRADED_SERVICE = "http://www.my-degraded-service.com";
    private static final String SAMPLE_SERVICE_NAME = "Foo service";
    private static final String SAMPLE_BASIC_AUTH_USER = "foo";
    private static final String SAMPLE_BASIC_AUTH_PASS = "bar";
    private static final String SAMPLE_BASIC_AUTH_BASE64 = new String(Base64.encodeBase64(
            (SAMPLE_BASIC_AUTH_USER + ":" + SAMPLE_BASIC_AUTH_PASS).getBytes(StandardCharsets.ISO_8859_1)
    ));
    private static final String SAMPLE_USER_AGENT = "FooUser 0.1";

    private final NotificationChannel channel = mock(NotificationChannel.class);
    private final List<NotificationChannel> channelList;
    private final NetworkConfig networkConfig = new NetworkConfig();
    private final DashboardConfiguration dashboardConfiguration = new DashboardConfiguration();

    public EndpointEvaluatorTest() {
        networkConfig.setHttpProxyHost("127.0.0.1");
        networkConfig.setHttpProxyPort(HOVERFLY_PORT);
        channelList = new ArrayList<>();
        channelList.add(channel);
    }

    @Test
    void noConnection() throws MalformedURLException {
        ServiceConfig config = new ServiceConfig();
        config.setUrl(INVALID_URL);
        config.setName(SAMPLE_SERVICE_NAME);
        NetworkConfig customNetworkConf = new NetworkConfig();
        customNetworkConf.setHttpProxyHost("127.0.0.2");
        customNetworkConf.setHttpProxyPort(80);
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConf, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel).openIncident(any());
    }

    @Test
    void successfulResponse(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE +SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }

    @Test
    void negativeResponse(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_DEGRADED_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(serverError())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_DEGRADED_SERVICE+SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    void incidentClosesAgainTest(Hoverfly hoverfly) throws MalformedURLException {
        InOrder inOrder = inOrder(channel);

        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(serverError())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE +SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        inOrder.verify(channel, calls(1)).openIncident(any());

        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success())));
        evaluator.evaluate();
        inOrder.verify(channel, calls(1)).closeIncident(any());
    }

    @Test
    void differentStatusCode(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(NO_CONTENT_PATH).willReturn(ResponseCreators.noContent())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE+NO_CONTENT_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    void basicAuthTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(ResponseCreators.success())));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setUserName(SAMPLE_BASIC_AUTH_USER);
        config.setPassword(SAMPLE_BASIC_AUTH_PASS);
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        hoverfly.verify(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).anyBody().header("Authorization", "Basic " + SAMPLE_BASIC_AUTH_BASE64));
        verify(channel, never()).openIncident(any());
    }

    @Test
    void validBodyTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success().body(
                jsonWithSingleQuotes("{'status':'UP'}")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }

    @Test
    void incidentDetails(Hoverfly hoverfly) throws MalformedURLException {
        String body = jsonWithSingleQuotes("{'status':'UP','details':{'foo':'bar'}}").body();
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(serverError().body(
                body))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, only()).openIncident(argThat(x -> {
            assertEquals(500, x.getHttpStatus());
            assertEquals(body, x.getBody());
            assertEquals(SAMPLE_SERVICE_NAME, x.getServiceName());
            assertEquals(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH, x.getUrl());
            assertTrue(x.isOpen());
            return true;
        }));
    }

    @Test
    void invalidBodyTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success().body(
                jsonWithSingleQuotes("{'status':'UNKNOWN','details':{'foo':'bar'}}")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    void emptyBodyTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success().body(
                jsonWithSingleQuotes("")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    void noStatusCodeTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(serverError().body(
                jsonWithSingleQuotes("{'status':'UP'}")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedActuatorStatus(Arrays.asList("UP"));
        EndpointEvaluator evaluator = new EndpointEvaluator(config, networkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }

    @Test
    void redirectFollowingDisabledTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE)
                .get(SAMPLE_HEALTH_PATH).willReturn(success().status(301).header("Location", SAMPLE_HEALTHY_SERVICE+SAMPLE_SUCCESS_PATH))
                .get(SAMPLE_SUCCESS_PATH).willReturn(success())
        ));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        NetworkConfig customNetworkConfig = new NetworkConfig();
        customNetworkConfig.setHttpProxyHost(networkConfig.getHttpProxyHost());
        customNetworkConfig.setHttpProxyPort(networkConfig.getHttpProxyPort());
        customNetworkConfig.setFollowRedirects(false);
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }

    @Test
    void redirectFollowingEnabledTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE)
                .get(SAMPLE_HEALTH_PATH).willReturn(success().status(301).header("Location", SAMPLE_HEALTHY_SERVICE+SAMPLE_SUCCESS_PATH))
                .get(SAMPLE_SUCCESS_PATH).willReturn(success())
        ));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        NetworkConfig customNetworkConfig = new NetworkConfig();
        customNetworkConfig.setHttpProxyHost(networkConfig.getHttpProxyHost());
        customNetworkConfig.setHttpProxyPort(networkConfig.getHttpProxyPort());
        customNetworkConfig.setFollowRedirects(true);
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }

    @Test
    void userAgentIsSet(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).willReturn(success().body(
                jsonWithSingleQuotes("")))));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        NetworkConfig customNetworkConfig = new NetworkConfig();
        customNetworkConfig.setHttpProxyHost(networkConfig.getHttpProxyHost());
        customNetworkConfig.setHttpProxyPort(networkConfig.getHttpProxyPort());
        customNetworkConfig.setFollowRedirects(true);
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConfig, dashboardConfiguration, channelList, SAMPLE_USER_AGENT);
        evaluator.evaluate();
        hoverfly.verify(service(SAMPLE_HEALTHY_SERVICE).get(SAMPLE_HEALTH_PATH).anyBody()
                .header("User-Agent", SAMPLE_USER_AGENT));
    }
}
