package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class EndpointEvaluator {
    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);

    RequestConfig requestConfig;
    ServiceConfig config;
    Incident currentIncident;
    NotificationChannel channel;

    public EndpointEvaluator(ServiceConfig config, NotificationChannel channel) {
        this.config = config;
        this.channel = channel;
        requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
    }

    public void evaluate() {
        logger.log(Level.INFO, "Evaluating health for {}", config.getName());
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpResponse response = client.execute(new HttpGet(config.getUrl()));
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log(Level.DEBUG, "Received status code {}", statusCode);

            if(!config.getAllowedStatusCodes().contains(statusCode)) {
                logger.log(Level.WARN, "Status code not matching allowed status codes");
                if(currentIncident == null || !currentIncident.isOpen()) {
                    HttpEntity entity = response.getEntity();
                    String body = EntityUtils.toString(entity);
                    currentIncident = new Incident(Incident.Type.UNEXPECTED_RESPONSE, channel);
                    currentIncident.setBody(body);
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
