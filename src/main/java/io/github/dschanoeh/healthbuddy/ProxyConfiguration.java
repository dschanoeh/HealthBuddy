package io.github.dschanoeh.healthbuddy;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class ProxyConfiguration {
    private static final Logger logger = LogManager.getLogger(ProxyConfiguration.class);

    @Getter
    @Setter
    private HttpHost httpProxy;
    @Getter
    @Setter
    private HttpHost httpsProxy;

    public static ProxyConfiguration fromNetworkConfig(NetworkConfig networkConfig) {
        ProxyConfiguration c = new ProxyConfiguration();

        HttpHost host = new HttpHost(networkConfig.getHttpProxyHost(), networkConfig.getHttpProxyPort());
        c.setHttpsProxy(host);
        c.setHttpProxy(host);

        return c;
    }

    public static ProxyConfiguration fromEnvironment() {
        ProxyConfiguration c = new ProxyConfiguration();

        String httpProxy = System.getenv("HTTP_PROXY");
        String httpsProxy = System.getenv("HTTPS_PROXY");

        c.setHttpProxy(proxyUrlToHost(httpProxy));
        c.setHttpsProxy(proxyUrlToHost(httpsProxy));
        return c;
    }

    private static HttpHost proxyUrlToHost(String proxyUrl) {
        if(proxyUrl == null) {
            return null;
        }
        try {
            URL url = new URL(proxyUrl);
            HttpHost proxy = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
            return proxy;
        } catch (MalformedURLException e) {
            logger.log(Level.ERROR, "Proxy URL is malformed: ", e);
            return null;
        }
    }

    public HttpHost getProxyForURL(URL url) {
        switch (url.getProtocol()) {
            case "http":
                HttpHost proxy = this.getHttpProxy();
                if(proxy != null) {
                    logger.log(Level.DEBUG, "Configuring proxy {} for url {}", proxy.toString(), url.toString());
                } else {
                    logger.log(Level.DEBUG, "Not configuring a proxy for url {}", url.toString());
                }
                return proxy;
            case "https":
                HttpHost proxy2 = this.getHttpsProxy();
                if(proxy2 != null) {
                    logger.log(Level.DEBUG, "Configuring proxy {} for url {}", proxy2.toString(), url.toString());
                } else {
                    logger.log(Level.DEBUG, "Not configuring a proxy for url {}", url.toString());
                }
                return proxy2;
        }
        logger.log(Level.ERROR, "URL has unknown scheme: {}", url);
        return null;
    }
}
