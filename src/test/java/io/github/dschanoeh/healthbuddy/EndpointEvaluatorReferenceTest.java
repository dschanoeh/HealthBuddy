package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.configuration.NetworkConfig;
import io.github.dschanoeh.healthbuddy.configuration.ServiceConfig;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyMode;
import io.specto.hoverfly.junit5.HoverflyExtension;
import io.specto.hoverfly.junit5.api.HoverflyConfig;
import io.specto.hoverfly.junit5.api.HoverflyCore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(HoverflyExtension.class)
@HoverflyCore(mode = HoverflyMode.SIMULATE, config = @HoverflyConfig(proxyPort = 8080))
@ExtendWith(SpringExtension.class)
class EndpointEvaluatorReferenceTest {
    protected static final Integer HOVERFLY_PORT = 8080;
    private static final String SAMPLE_HEALTHY_SERVICE = "http://www.my-service.com";
    private static final String SAMPLE_HEALTH_PATH = "/health";
    private static final String SAMPLE_REFERENCE_PATH = "/reference";
    private static final String SAMPLE_SERVICE_NAME = "Foo service";
    private static final String SAMPLE_USER_AGENT = "FooUser 0.1";

    private final NotificationChannel channel = mock(NotificationChannel.class);
    private final List<NotificationChannel> channelList;

    @Autowired
    private NetworkConfig networkConfig;
    @Autowired
    @Qualifier("getTestReferenceEndpointEvaluator")
    private ReferenceEndpointEvaluator referenceEndpointEvaluator;

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ReferenceEndpointEvaluator getTestReferenceEndpointEvaluator() {
            return new ReferenceEndpointEvaluator(SAMPLE_HEALTHY_SERVICE+SAMPLE_REFERENCE_PATH);
        }

        @Bean
        public NetworkConfig getTestNetworkConfig() {
            NetworkConfig n = new NetworkConfig();
            n.setHttpProxyHost("127.0.0.1");
            n.setHttpProxyPort(HOVERFLY_PORT);
            return n;
        }

        @Bean(name = "userAgent")
        public String userAgent() {
            return SAMPLE_USER_AGENT;
        }
    }

    public EndpointEvaluatorReferenceTest() {
        channelList = new ArrayList<>();
        channelList.add(channel);
    }

    @Test
    void referenceEndpointSuppressionTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE)
                .get(SAMPLE_HEALTH_PATH).willReturn(serverError())
                .get(SAMPLE_REFERENCE_PATH).willReturn(serverError())
        ));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        NetworkConfig customNetworkConfig = new NetworkConfig();
        customNetworkConfig.setHttpProxyHost(networkConfig.getHttpProxyHost());
        customNetworkConfig.setHttpProxyPort(networkConfig.getHttpProxyPort());
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConfig, channelList, SAMPLE_USER_AGENT);
        evaluator.setReferenceEndpointEvaluator(referenceEndpointEvaluator);
        evaluator.evaluate();
        verify(channel, never()).openIncident(any());
    }

    @Test
    void referenceEndpointNoSuppressionTest(Hoverfly hoverfly) throws MalformedURLException {
        hoverfly.simulate(dsl(service(SAMPLE_HEALTHY_SERVICE)
                .get(SAMPLE_HEALTH_PATH).willReturn(serverError())
                .get(SAMPLE_REFERENCE_PATH).willReturn(success())
        ));
        ServiceConfig config = new ServiceConfig();
        config.setUrl(SAMPLE_HEALTHY_SERVICE + SAMPLE_HEALTH_PATH);
        config.setName(SAMPLE_SERVICE_NAME);
        config.setAllowedStatusCodes(Arrays.asList(200));
        NetworkConfig customNetworkConfig = new NetworkConfig();
        customNetworkConfig.setHttpProxyHost(networkConfig.getHttpProxyHost());
        customNetworkConfig.setHttpProxyPort(networkConfig.getHttpProxyPort());
        EndpointEvaluator evaluator = new EndpointEvaluator(config, customNetworkConfig, channelList, SAMPLE_USER_AGENT);
        evaluator.setReferenceEndpointEvaluator(referenceEndpointEvaluator);
        evaluator.evaluate();
        verify(channel, only()).openIncident(any());
    }
}
