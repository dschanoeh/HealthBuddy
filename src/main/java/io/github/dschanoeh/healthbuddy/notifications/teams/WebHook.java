package io.github.dschanoeh.healthbuddy.notifications.teams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dschanoeh.healthbuddy.configuration.NetworkConfig;
import io.github.dschanoeh.healthbuddy.configuration.ProxyConfiguration;
import io.github.dschanoeh.healthbuddy.notifications.AbstractNotificationReceiver;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class WebHook extends AbstractNotificationReceiver {
    private static final Logger logger = LogManager.getLogger(WebHook.class);
    private static final int WEBHOOK_CONNECT_TIMEOUT_MS = 5000;

    private final WebHookConfiguration configuration;
    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private final RequestConfig requestConfig;
    private final CloseableHttpClient httpClient;
    ObjectMapper mapper = new ObjectMapper();

    public WebHook(WebHookConfiguration configuration, NetworkConfig networkConfiguration) {
        this.configuration = configuration;
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(WEBHOOK_CONNECT_TIMEOUT_MS);
        ProxyConfiguration proxyConfiguration = networkConfiguration.getProxyConfiguration();
        HttpHost proxy = proxyConfiguration.getProxyForURL(configuration.getUrl());
        if(proxy != null) {
            builder.setProxy(proxy);
            ProxyConfiguration.Authentication auth = proxyConfiguration.getAuthenticationForURL(configuration.getUrl());
            if (auth != null) {
                credentialsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()),
                        new UsernamePasswordCredentials(auth.getUser(), auth.getPassword()));
            }
        }
        requestConfig = builder.build();
        this.httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setDefaultCredentialsProvider(credentialsProvider).build();
        this.setEnvironmentPattern(configuration.getCompiledEnvironmentPattern());
    }

    public void send(TeamsMessage message) {
        String messageString;
        try {
            messageString = mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.log(Level.ERROR, "Could not serialize message: ", e);
            return;
        }
        logger.log(Level.INFO, "Message: {}", messageString);

        HttpPost httpPost = new HttpPost(configuration.getUrl().toString());
        httpPost.setHeader("Content-type", "application/json");
        try {
            StringEntity entity = new StringEntity(messageString);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.log(Level.DEBUG, "Received status code {}", statusCode);

                if (statusCode != 200) {
                    HttpEntity responseEntity = response.getEntity();
                    String body = EntityUtils.toString(responseEntity);
                    logger.log(Level.ERROR, "Failed to Post to webhook. Response: {}", body);
                }
            } catch (ClientProtocolException e) {
                logger.log(Level.ERROR, "Failed to Post to webhook: ", e);
            } catch (IOException e) {
                logger.log(Level.ERROR, "Failed to Connect to webhook: ", e);
            }
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.ERROR, "Encoding not supported: ", ex);
        }
    }
}
