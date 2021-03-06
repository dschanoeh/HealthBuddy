package io.github.dschanoeh.healthbuddy.notifications.teams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.NetworkConfig;
import io.github.dschanoeh.healthbuddy.ProxyConfiguration;
import io.github.dschanoeh.healthbuddy.ServiceMonitor;
import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class TeamsNotificationChannel implements NotificationChannel {
    private static final Logger logger = LogManager.getLogger(ServiceMonitor.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss z");

    TeamsConfiguration configuration;
    RequestConfig requestConfig;
    ObjectMapper mapper = new ObjectMapper();
    private final NetworkConfig networkConfig;
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private final HttpClientContext context = HttpClientContext.create();

    public TeamsNotificationChannel(TeamsConfiguration configuration, NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
        logger.log(Level.INFO, "TeamsNotificationChannel created");
        this.configuration = configuration;

        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(5 * 1000);
        ProxyConfiguration proxyConfiguration = networkConfig.getProxyConfiguration();
        HttpHost proxy = proxyConfiguration.getProxyForURL(configuration.getWebHookURL());
        if(proxy != null) {
            builder.setProxy(proxy);
        }
        ProxyConfiguration.Authentication auth = proxyConfiguration.getAuthenticationForURL(configuration.getWebHookURL());
        if (auth != null) {
            credentialsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()),
                    new UsernamePasswordCredentials(auth.getUser(), auth.getPassword()));
            context.setCredentialsProvider(credentialsProvider);
        }
        requestConfig = builder.build();
    }
    @Override
    public void openIncident(Incident i) {
        logger.log(Level.INFO, "Sending openIncident notification");
        TeamsMessage message = new TeamsMessage();
        message.setThemeColor(TeamsMessage.COLOR_RED);
        message.setTitle("New Incident");
        message.setSummary(String.format("A new incident for the service '%s' was opened", i.getServiceName()));
        TeamsMessageSection section = new TeamsMessageSection();
        message.getSections().add(section);

        switch(i.getType()) {
            case UNEXPECTED_RESPONSE:
                section.setActivityTitle("Unexpected response from observed endpoint");
                section.getFacts().add(new TeamsMessageSection.Fact("Service", i.getServiceName()));
                if(i.getEnvironment() != null) {
                    section.getFacts().add(new TeamsMessageSection.Fact("Environment", i.getEnvironment()));
                }
                if(i.getBody() != null) {
                    section.getFacts().add(new TeamsMessageSection.Fact("Response", i.getBody()));
                }
                if(i.getHttpStatus() != null) {
                    section.getFacts().add(new TeamsMessageSection.Fact("Status Code", String.valueOf(i.getHttpStatus())));
                }
                if(i.getStartDate() != null) {
                    section.getFacts().add(new TeamsMessageSection.Fact("Start Date", dateTimeFormatter.format(i.getStartDate())));
                }
                break;
            case NOT_REACHABLE:
                section.setActivityTitle("The observed endpoint is not reachable");
                section.getFacts().add(new TeamsMessageSection.Fact("Service", i.getServiceName()));
                if(i.getEnvironment() != null) {
                    section.getFacts().add(new TeamsMessageSection.Fact("Environment", i.getEnvironment()));
                }
                if(i.getStartDate() != null) {
                    section.getFacts().add(new TeamsMessageSection.Fact("Start Date", dateTimeFormatter.format(i.getStartDate())));
                }
                break;
            default:
                logger.log(Level.ERROR, "Received message of unknown type");
                return;
        }

        sendMessage(message);
    }

    @Override
    public void closeIncident(Incident i) {
        logger.log(Level.INFO, "Sending closeIncident notification");
        TeamsMessage message = new TeamsMessage();
        message.setThemeColor(TeamsMessage.COLOR_GREEN);
        message.setTitle("Incident Resolved");
        message.setSummary(String.format("The incident for the service '%s' has been resolved", i.getServiceName()));
        TeamsMessageSection section = new TeamsMessageSection();
        message.getSections().add(section);

        section.getFacts().add(new TeamsMessageSection.Fact("Service", i.getServiceName()));
        if(i.getEnvironment() != null) {
            section.getFacts().add(new TeamsMessageSection.Fact("Environment", i.getEnvironment()));
        }
        if(i.getStartDate() != null) {
            section.getFacts().add(new TeamsMessageSection.Fact("Start Date", dateTimeFormatter.format(i.getStartDate())));
        }
        if(i.getEndDate() != null) {
            section.getFacts().add(new TeamsMessageSection.Fact("End Date", dateTimeFormatter.format(i.getEndDate())));
        }
        if(i.getEndDate() != null && i.getStartDate() != null) {
            Duration duration = Duration.between(i.getStartDate(), i.getEndDate());
            Long s = duration.getSeconds();
            String durationString = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            section.getFacts().add(new TeamsMessageSection.Fact("Duration", durationString));
        }
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
            HttpPost httpPost = new HttpPost(configuration.getWebHookURL().toString());
            httpPost.setHeader("Content-type", "application/json");
            StringEntity entity = new StringEntity(messageString);
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost, context);
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
