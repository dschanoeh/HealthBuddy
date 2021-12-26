package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.configuration.NetworkConfig;
import io.github.dschanoeh.healthbuddy.configuration.ProxyConfiguration;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;

public class ReferenceEndpointEvaluator {
    private static final Logger logger = LogManager.getLogger(ReferenceEndpointEvaluator.class);
    private final String url;
    @Autowired
    @Qualifier("userAgent")
    private String userAgent;
    @Autowired(required = false)
    private NetworkConfig networkConfig;
    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private CloseableHttpClient httpClient;

    public ReferenceEndpointEvaluator(String url) {
        this.url = url;
    }

    public boolean isUp() {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            int statusCode = response.getStatusLine().getStatusCode();
            logger.log(Level.INFO, "Received status code {} from reference endpoint", statusCode);
            if(statusCode == 200) {
                return true;
            }
        } catch (ClientProtocolException e) {
            logger.log(Level.INFO, "Received client protocol exception when calling reference endpoint", e);
        } catch (IOException e) {
            logger.log(Level.INFO, "Received IO exception when calling reference endpoint", e);
        }
        return false;
    }

    @PostConstruct
    public void setUp() throws MalformedURLException {
        logger.log(Level.INFO, "Setting up reference endpoint evaluator");
        java.net.URL parsedUrl = new java.net.URL(this.url);
        RequestConfig.Builder builder = RequestConfig.custom();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        if (networkConfig != null) {
            logger.log(Level.DEBUG, "Network configuration was provided");
            ProxyConfiguration proxyConfiguration = networkConfig.getProxyConfiguration();
            HttpHost proxy = proxyConfiguration.getProxyForURL(parsedUrl);
            if (proxy != null) {
                builder.setProxy(proxy);
                ProxyConfiguration.Authentication auth = proxyConfiguration.getAuthenticationForURL(parsedUrl);
                if (auth != null) {
                    credentialsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()),
                            new UsernamePasswordCredentials(auth.getUser(), auth.getPassword()));
                }
            }
            if (networkConfig.getTimeout() != null) {
                builder.setConnectTimeout(networkConfig.getTimeout());
            }
            if (Boolean.FALSE.equals(networkConfig.getFollowRedirects())) {
                httpClientBuilder.disableRedirectHandling();
            }
        }

        RequestConfig requestConfig = builder.build();

        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        httpClientBuilder.setUserAgent(this.userAgent);

        this.httpClient = httpClientBuilder.build();
    }
}
