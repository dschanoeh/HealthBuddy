package io.github.dschanoeh.healthbuddy.notifications.teams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.ServiceMonitor;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class TeamsNotificationChannel implements NotificationChannel {
    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);

    TeamsConfiguration configuration;
    RequestConfig requestConfig;
    ObjectMapper mapper = new ObjectMapper();

    public TeamsNotificationChannel(TeamsConfiguration configuration) {
        logger.log(Level.INFO, "TeamsNotificationChannel created");
        this.configuration = configuration;
        requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
    }
    @Override
    public void openIncident(Incident i) {
        logger.log(Level.INFO, "Sending openIncident notification");
        TeamsMessage message = new TeamsMessage();
        message.setThemeColor(TeamsMessage.COLOR_RED);
        message.setTitle(String.format("New Incident (%s) for %s", i.getType().toString(), i.getServiceName()));
        switch(i.getType()) {
            case UNEXPECTED_RESPONSE:
                message.setText(String.format("Response: %s",i.getBody()));
                break;
            case NOT_REACHABLE:
                message.setText("Service not reachable.");
                break;
        }

        sendMessage(message);
    }

    @Override
    public void closeIncident(Incident i) {
        logger.log(Level.INFO, "Sending closeIncident notification");
        TeamsMessage message = new TeamsMessage();
        message.setThemeColor(TeamsMessage.COLOR_GREEN);
        message.setText("The incident has been resolved.");
        sendMessage(message);
    }

    private void sendMessage(TeamsMessage message) {
        String messageString;
        try {
            messageString = mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.log(Level.ERROR, "Could not serialize message: ", e);
            return;
        }
        logger.log(Level.INFO, "Message: {}", messageString);

        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpPost httpPost = new HttpPost(configuration.getWebHookURL());
            httpPost.setHeader("Content-type", "application/json");
            StringEntity entity = new StringEntity(messageString);
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log(Level.DEBUG, "Received status code {}", statusCode);

            if(statusCode != 200) {
                HttpEntity responseEntity = response.getEntity();
                String body = EntityUtils.toString(responseEntity);
                logger.log(Level.ERROR, "Failed to Post to webhook. Response: {}", body);
            }
        } catch (ClientProtocolException e) {
            logger.log(Level.ERROR, "Failed to Post to webhook: ", e);
        } catch (IOException e) {
            logger.log(Level.ERROR, "Failed to Connect to webhook: ", e);
        }
    }
}
