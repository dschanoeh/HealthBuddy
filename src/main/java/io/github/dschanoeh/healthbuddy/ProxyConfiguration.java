package io.github.dschanoeh.healthbuddy;

import lombok.AllArgsConstructor;
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

    @AllArgsConstructor
    public static class Authentication {
        @Getter
        @Setter
        private String user;
        @Getter
        @Setter
        private String password;
    }

    @Getter
    @Setter
    private HttpHost httpProxy;
    @Getter
    @Setter
    private HttpHost httpsProxy;
    @Getter
    @Setter
    private Authentication httpAuthentication;
    @Getter
    @Setter
    private Authentication httpsAuthentication;

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
        c.setHttpAuthentication(proxyUrlToAuthentication(httpProxy));
        c.setHttpsAuthentication(proxyUrlToAuthentication(httpsProxy));
        return c;
    }

    public static ProxyConfiguration fromProperties() {
        ProxyConfiguration c = new ProxyConfiguration();

        String httpHost = System.getProperty("http.proxyHost");
        String httpPort = System.getProperty("http.proxyPort");
        String httpUser = System.getProperty("http.proxyUser");
        String httpPassword = System.getProperty("http.proxyPassword");

        String httpsHost = System.getProperty("https.proxyHost");
        String httpsPort = System.getProperty("https.proxyPort");
        String httpsUser = System.getProperty("https.proxyUser");
        String httpsPassword = System.getProperty("https.proxyPassword");

        if(httpHost != null && httpPort != null) {
            logger.log(Level.DEBUG, "Configuring http proxy...");
            HttpHost httpProxy = new HttpHost(httpHost, Integer.parseInt(httpPort), "http");
            c.setHttpProxy(httpProxy);
            if(httpUser != null && httpPassword != null) {
                logger.log(Level.DEBUG, "With authentication...");
                c.setHttpAuthentication(new Authentication(httpUser, httpPassword));
            }
        }

        if(httpsHost != null && httpsPort != null) {
            logger.log(Level.DEBUG, "Configuring https proxy...");
            HttpHost httpsProxy = new HttpHost(httpsHost, Integer.parseInt(httpsPort), "https");
            c.setHttpsProxy(httpsProxy);
            if(httpsUser != null && httpsPassword != null) {
                logger.log(Level.DEBUG, "With authentication...");
                c.setHttpsAuthentication(new Authentication(httpsUser, httpsPassword));
            }
        }
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

    private static Authentication proxyUrlToAuthentication(String proxyUrl) {
        if(proxyUrl == null) {
            return null;
        }
        try {
            URL url = new URL(proxyUrl);
            String userInfo = url.getUserInfo();
            if(userInfo.contains(":")) {
                String user = userInfo.substring(0, userInfo.indexOf(':'));
                String password = userInfo.substring(userInfo.indexOf(':') + 1);
                Authentication a = new Authentication(user, password);
                return a;
            } else {
                logger.log(Level.WARN, "Proxy user info must include user and password");
                return null;
            }
        } catch (MalformedURLException e) {
            logger.log(Level.WARN, "Proxy URL is malformed: ", e);
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

    public Authentication getAuthenticationForURL(URL url) {
        switch (url.getProtocol()) {
            case "http":
                Authentication auth = this.getHttpAuthentication();
                if(auth != null) {
                    logger.log(Level.DEBUG, "Using proxy authentication for url {}", url.toString());
                } else {
                    logger.log(Level.DEBUG, "Using no proxy authentication for url {}", url.toString());
                }
                return auth;
            case "https":
                Authentication auth2 = this.getHttpsAuthentication();
                if(auth2 != null) {
                    logger.log(Level.DEBUG, "Using proxy authentication for url {}", url.toString());
                } else {
                    logger.log(Level.DEBUG, "Using no proxy authentication for url {}", url.toString());
                }
                return auth2;
        }
        logger.log(Level.ERROR, "URL has unknown scheme: {}", url);
        return null;
    }
}
