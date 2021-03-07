package io.github.dschanoeh.healthbuddy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class EndpointEvaluator {
    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);

    private RequestConfig requestConfig;
    private final ServiceConfig config;
    private Incident currentIncident;
    private final NotificationChannel channel;
    private final NetworkConfig networkConfig;
    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private final AuthCache cache = new BasicAuthCache();
    private final HttpClientContext context = HttpClientContext.create();

    public EndpointEvaluator(ServiceConfig config, NetworkConfig networkConfig, NotificationChannel channel) throws MalformedURLException {
        this.config = config;
        this.channel = channel;
        this.networkConfig = networkConfig;
        setupRequestConfig();
    }

    private void setupRequestConfig() throws MalformedURLException {
        URL url = new URL(config.getUrl());
        RequestConfig.Builder builder = RequestConfig.custom();

        if(networkConfig != null) {
            ProxyConfiguration proxyConfiguration = networkConfig.getProxyConfiguration();
            HttpHost proxy = proxyConfiguration.getProxyForURL(url);
            if(proxy != null) {
                builder.setProxy(proxy);
            }
            ProxyConfiguration.Authentication auth = proxyConfiguration.getAuthenticationForURL(url);
            if (auth != null) {
                credentialsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()),
                        new UsernamePasswordCredentials(auth.getUser(), auth.getPassword()));
            }
            if(networkConfig.getTimeout() != null) {
                builder.setConnectTimeout(networkConfig.getTimeout());
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

        this.requestConfig = builder.build();
    }

    public void evaluate() {
        logger.log(Level.INFO, "Evaluating health for {}", config.getName());
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setDefaultCredentialsProvider(credentialsProvider).build()) {
            HttpResponse response = client.execute(new HttpGet(config.getUrl()), context);
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log(Level.DEBUG, "Received status code {}", statusCode);
            HttpEntity entity = response.getEntity();
            String body = null;
            if(entity != null) {
                body = EntityUtils.toString(entity);
                logger.log(Level.DEBUG, "Received body {}", body);
            }

            Boolean validStatus = true;
            Boolean validBody = true;

            // Optionally validate status code
            if(config.getAllowedStatusCodes() != null) {
                if (!config.getAllowedStatusCodes().contains(statusCode)) {
                    validStatus = false;
                    logger.log(Level.WARN, "Status code not matching allowed status codes");
                }
            }

            // Optionally validate actuator status
            if(config.getAllowedActuatorStatus() != null) {
                validBody = validateBody(body);
                if(!validBody) {
                    logger.log(Level.WARN, "Body not matching criteria");
                }
            }

            if(!(validStatus && validBody)) {
                if(currentIncident == null || !currentIncident.isOpen()) {
                    currentIncident = new Incident(Incident.Type.UNEXPECTED_RESPONSE, channel);
                    if(body != null) {
                        currentIncident.setBody(body);
                    }
                    currentIncident.setHttpStatus(statusCode);
                    currentIncident.setServiceName(config.getName());
                    currentIncident.setEnvironment(config.getEnvironment());
                    currentIncident.open();
                }
            } else {
                if(currentIncident != null && currentIncident.isOpen()) {
                    currentIncident.close();
                }
            }
        } catch (ClientProtocolException e) {
            logger.log(Level.WARN, "Received a client protocol exception");
            if(currentIncident == null || !currentIncident.isOpen()) {
                currentIncident = new Incident(Incident.Type.NOT_REACHABLE, channel);
                currentIncident.setServiceName(config.getName());
                currentIncident.setEnvironment(config.getEnvironment());
                currentIncident.open();
            }
        } catch (IOException e) {
            logger.log(Level.WARN, "Could not connect to endpoint");
            if(currentIncident == null || !currentIncident.isOpen()) {
                currentIncident = new Incident(Incident.Type.NOT_REACHABLE, channel);
                currentIncident.setServiceName(config.getName());
                currentIncident.setEnvironment(config.getEnvironment());
                currentIncident.open();
            }
        }
    }

    private boolean validateBody(String body) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ActuatorHealthResponse response = objectMapper.readValue(body, ActuatorHealthResponse.class);
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
}
