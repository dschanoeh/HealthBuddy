package io.github.dschanoeh.healthbuddy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dschanoeh.healthbuddy.configuration.NetworkConfig;
import io.github.dschanoeh.healthbuddy.configuration.ProxyConfiguration;
import io.github.dschanoeh.healthbuddy.configuration.ServiceConfig;
import io.github.dschanoeh.healthbuddy.dto.ActuatorHealthResponseDTO;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class EndpointEvaluator {
    private static final Logger logger = LogManager.getLogger(EndpointEvaluator.class);
    private static final String SERVICE_TC_IDENTIFIER = "service";
    private static final String ENVIRONMENT_TC_IDENTIFIER = "environment";

    private final ServiceConfig config;
    @Getter
    private Incident currentIncident;
    private final List<NotificationChannel> channels;
    private final NetworkConfig networkConfig;
    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private final AuthCache cache = new BasicAuthCache();
    private final HttpClientContext context = HttpClientContext.create();
    private CloseableHttpClient httpClient;
    private final String userAgent;
    @Getter
    @Setter
    private ReferenceEndpointEvaluator referenceEndpointEvaluator;

    public EndpointEvaluator(ServiceConfig config, NetworkConfig networkConfig, List<NotificationChannel> channels, String userAgent) throws MalformedURLException {
        this.config = config;
        this.channels = channels;
        this.networkConfig = networkConfig;
        this.userAgent = userAgent;
        setupRequestConfig();
    }

    private void setupRequestConfig() throws MalformedURLException {
        URL url = new URL(config.getUrl());
        RequestConfig.Builder builder = RequestConfig.custom();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        if(networkConfig != null) {
            ProxyConfiguration proxyConfiguration = networkConfig.getProxyConfiguration();
            HttpHost proxy = proxyConfiguration.getProxyForURL(url);
            if(proxy != null) {
                builder.setProxy(proxy);
                ProxyConfiguration.Authentication auth = proxyConfiguration.getAuthenticationForURL(url);
                if (auth != null) {
                    credentialsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()),
                            new UsernamePasswordCredentials(auth.getUser(), auth.getPassword()));
                }
            }
            if(networkConfig.getTimeout() != null) {
                builder.setConnectTimeout(networkConfig.getTimeout());
            }
            if(Boolean.FALSE.equals(networkConfig.getFollowRedirects())) {
                httpClientBuilder.disableRedirectHandling();
            }
        }

        if(config.getUserName() != null && config.getPassword() != null) {
            logger.log(Level.INFO, "Basic auth was configured - setting it up.");

            // Store the credentials for use during GET
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(config.getUserName(), config.getPassword());
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);

            // Set up preemptive Basic Authentication for the host
            HttpHost httpHost = new HttpHost(url.getHost());
            cache.put(httpHost, new BasicScheme());

            context.setCredentialsProvider(credentialsProvider);
            context.setAuthCache(cache);
        }

        RequestConfig requestConfig = builder.build();

        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        httpClientBuilder.setUserAgent(this.userAgent);

        this.httpClient = httpClientBuilder.build();
    }

    public void evaluate() {
        ThreadContext.put(SERVICE_TC_IDENTIFIER, config.getName());
        ThreadContext.put(ENVIRONMENT_TC_IDENTIFIER, config.getEnvironment());
        logger.log(Level.INFO, "Evaluating health...");
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(config.getUrl()), context)) {
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log(Level.DEBUG, "Received status code {}", statusCode);
            HttpEntity entity = response.getEntity();
            String body = null;
            Boolean isJson = false;
            if(entity != null) {
                body = EntityUtils.toString(entity);
                final Header contentType = entity.getContentType();
                if (contentType != null) {
                    for (final HeaderElement element : contentType.getElements()) {
                        if (element.getName().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
                            logger.log(Level.INFO, "Json response received");
                            isJson = true;
                            break;
                        }
                    }
                }
                logger.log(Level.DEBUG, "Received body {}", body);
            }

            Boolean validStatus = true;
            Boolean validBody = true;

            // Optionally validate status code
            if(config.getAllowedStatusCodes() != null && !config.getAllowedStatusCodes().contains(statusCode)) {
                validStatus = false;
                logger.log(Level.WARN, "Status code not matching allowed status codes");
            }

            // Optionally validate actuator status
            if(config.getAllowedActuatorStatus() != null) {
                validBody = validateBody(body);
                if(Boolean.FALSE.equals(validBody)) {
                    logger.log(Level.WARN, "Body not matching criteria");
                }
            }

            /* If we consider the status or the body not valid, we need to think about opening an incident... */
            if(!(validStatus && validBody)) {
                /* ...but only if the reference endpoint isn't also down, or we already have an incident open */
                if(checkReferenceEndpoint() && (currentIncident == null || !currentIncident.isOpen())) {
                    logger.log(Level.INFO, "Creating new incident...");
                    currentIncident = new Incident(Incident.Type.UNEXPECTED_RESPONSE, channels);
                    if(body != null) {
                        if(Boolean.TRUE.equals(isJson)) {
                            currentIncident.setBody(prettyPrintJson(body));
                        } else {
                            currentIncident.setBody(body);
                        }
                    }
                    currentIncident.setUrl(config.getUrl());
                    currentIncident.setHttpStatus(statusCode);
                    currentIncident.setServiceName(config.getName());
                    currentIncident.setEnvironment(config.getEnvironment());
                    currentIncident.open();
                }
            /* If we got a valid response, we can close the incident if there was any */
            } else {
                if(currentIncident != null && currentIncident.isOpen()) {
                    currentIncident.close();
                }
            }
        } catch (ClientProtocolException e) {
            logger.log(Level.WARN, "Received a client protocol exception");
            if(currentIncident == null || !currentIncident.isOpen()) {
                currentIncident = new Incident(Incident.Type.NOT_REACHABLE, channels);
                currentIncident.setServiceName(config.getName());
                currentIncident.setEnvironment(config.getEnvironment());
                currentIncident.setUrl(config.getUrl());
                currentIncident.open();
            }
        } catch (IOException e) {
            logger.log(Level.WARN, "Could not connect to endpoint");
            if(currentIncident == null || !currentIncident.isOpen()) {
                currentIncident = new Incident(Incident.Type.NOT_REACHABLE, channels);
                currentIncident.setServiceName(config.getName());
                currentIncident.setEnvironment(config.getEnvironment());
                currentIncident.setUrl(config.getUrl());
                currentIncident.open();
            }
        }
    }

    public Boolean isUp() {
        if(currentIncident == null) {
            return true;
        }
        return !currentIncident.isOpen();
    }

    private boolean validateBody(String body) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ActuatorHealthResponseDTO response = objectMapper.readValue(body, ActuatorHealthResponseDTO.class);
            if(!config.getAllowedActuatorStatus().contains(response.getStatus())) {
                logger.log(Level.WARN, "Received status {} is not an allowed status", response.getStatus());
                return false;
            }
        } catch (JsonProcessingException ex) {
            logger.log(Level.WARN, "Was not able to parse actuator response", ex);
            return false;
        }
        return true;
    }

    private boolean checkReferenceEndpoint() {
        if(referenceEndpointEvaluator != null) {
            Boolean result = referenceEndpointEvaluator.isUp();
            if(result) {
                logger.log(Level.INFO, "Checked reference endpoint, which is up. Letting alert through.");
            } else {
                logger.log(Level.INFO, "Checked reference endpoint, which is also down. Suppressing alert.");
            }
            return result;
        }
        return true;
    }

    private String prettyPrintJson(String input) {
        if (input == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(input).toPrettyString();
        } catch (JsonProcessingException e) {
            logger.log(Level.WARN, "Failed to prettify json - returning unmodified",e);
            return input;
        }
    }
}
