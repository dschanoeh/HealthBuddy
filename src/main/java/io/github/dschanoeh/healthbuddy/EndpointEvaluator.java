package io.github.dschanoeh.healthbuddy;

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
import org.apache.http.client.utils.URIUtils;
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
import java.net.URI;
import java.net.URISyntaxException;

public class EndpointEvaluator {
    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);

    private RequestConfig requestConfig;
    private ServiceConfig config;
    private Incident currentIncident;
    private NotificationChannel channel;
    private NetworkConfig networkConfig;
    private CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private AuthCache cache = new BasicAuthCache();
    private HttpClientContext context = HttpClientContext.create();

    public EndpointEvaluator(ServiceConfig config, NetworkConfig networkConfig, NotificationChannel channel) throws URISyntaxException {
        this.config = config;
        this.channel = channel;
        this.networkConfig = networkConfig;
        setupRequestConfig();
    }

    private void setupRequestConfig() throws URISyntaxException {
        URI uri = new URI(config.getUrl());
        RequestConfig.Builder builder = RequestConfig.custom();

        if(networkConfig.getHttpProxyHost() != null) {
            HttpHost proxy = new HttpHost(networkConfig.getHttpProxyHost(), networkConfig.getHttpProxyPort());
            builder.setProxy(proxy);
        }

        if(config.getUserName() != null && config.getPassword() != null) {
            logger.log(Level.INFO, "Basic auth was configured - setting it up.");

            // Store the credentials for use during GET
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(config.getUserName(), config.getPassword());
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);

            // Set up preemptive Basic Authentication for the host
            HttpHost httpHost = URIUtils.extractHost(uri);
            cache.put(httpHost, new BasicScheme());

            context.setCredentialsProvider(credentialsProvider);
            context.setAuthCache(cache);
        }

        builder.setConnectTimeout(networkConfig.getTimeout());
        this.requestConfig = builder.build();
    }

    public void evaluate() {
        logger.log(Level.INFO, "Evaluating health for {}", config.getName());
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpResponse response = client.execute(new HttpGet(config.getUrl()), context);
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log(Level.DEBUG, "Received status code {}", statusCode);

            if(!config.getAllowedStatusCodes().contains(statusCode)) {
                logger.log(Level.WARN, "Status code not matching allowed status codes");
                if(currentIncident == null || !currentIncident.isOpen()) {
                    HttpEntity entity = response.getEntity();
                    currentIncident = new Incident(Incident.Type.UNEXPECTED_RESPONSE, channel);
                    if(entity != null) {
                        String body = EntityUtils.toString(entity);
                        currentIncident.setBody(body);
                    }
                    currentIncident.setHttpStatus(statusCode);
                    currentIncident.setServiceName(config.getName());
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
                currentIncident.open();
            }
        } catch (IOException e) {
            logger.log(Level.WARN, "Could not connect to endpoint");
            if(currentIncident == null || !currentIncident.isOpen()) {
                currentIncident = new Incident(Incident.Type.NOT_REACHABLE, channel);
                currentIncident.setServiceName(config.getName());
                currentIncident.open();
            }
        }

    }
}
