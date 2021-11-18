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
import java.util.ArrayList;
import java.util.List;

public class ProxyConfiguration {
    private static final Logger logger = LogManager.getLogger(ProxyConfiguration.class);
    private static final String CONFIGURING_PROXY_PATTERN = "Configuring proxy {} for url {}";
    private static final String NOT_CONFIGURING_PROXY_PATTERN = "Not configuring a proxy for url {}";

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
    @Getter
    @Setter
    private List<String> nonProxyHosts;

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
        String httpNonProxyHosts = System.getenv("NO_PROXY");

        c.setHttpProxy(proxyUrlToHost(httpProxy));
        c.setHttpsProxy(proxyUrlToHost(httpsProxy));
        c.setHttpAuthentication(proxyUrlToAuthentication(httpProxy));
        c.setHttpsAuthentication(proxyUrlToAuthentication(httpsProxy));

        if(httpNonProxyHosts != null) {
            logger.log(Level.DEBUG, "Configuring non proxy hosts...");
            ArrayList<String> hosts = new ArrayList<>();
            String[] substrings = httpNonProxyHosts.split(",");
            for(String s : substrings) {
                if(s.startsWith("*")) {
                    s = s.substring(1);
                }
                hosts.add(s);
            }
            c.setNonProxyHosts(hosts);
        }
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

        String httpNonProxyHosts = System.getProperty("http.nonProxyHosts");

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
            HttpHost httpsProxy = new HttpHost(httpsHost, Integer.parseInt(httpsPort), "http");
            c.setHttpsProxy(httpsProxy);
            if(httpsUser != null && httpsPassword != null) {
                logger.log(Level.DEBUG, "With authentication...");
                c.setHttpsAuthentication(new Authentication(httpsUser, httpsPassword));
            }
        }

        if(httpNonProxyHosts != null) {
            logger.log(Level.DEBUG, "Configuring non proxy hosts...");
            ArrayList<String> hosts = new ArrayList<>();
            String[] substrings = httpNonProxyHosts.split("\\|");
            for(String s : substrings) {
                if(s.startsWith("*")) {
                    s = s.substring(1);
                }
                hosts.add(s);
            }
            c.setNonProxyHosts(hosts);
        }
        return c;
    }

    private static HttpHost proxyUrlToHost(String proxyUrl) {
        if(proxyUrl == null) {
            return null;
        }
        try {
            URL url = new URL(proxyUrl);
            return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
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
            if(userInfo != null && userInfo.contains(":")) {
                String user = userInfo.substring(0, userInfo.indexOf(':'));
                String password = userInfo.substring(userInfo.indexOf(':') + 1);
                return new Authentication(user, password);
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
        if(nonProxyHosts != null) {
            for(String host : nonProxyHosts) {
                if(url.getHost().endsWith(host)) {
                    logger.log(Level.DEBUG, NOT_CONFIGURING_PROXY_PATTERN, url);
                    return null;
                }
            }
        }
        switch (url.getProtocol()) {
            case "http":
                HttpHost proxy = this.getHttpProxy();
                if(proxy != null) {
                    logger.log(Level.DEBUG, CONFIGURING_PROXY_PATTERN, proxy, url);
                } else {
                    logger.log(Level.DEBUG, NOT_CONFIGURING_PROXY_PATTERN, url);
                }
                return proxy;
            case "https":
                HttpHost proxy2 = this.getHttpsProxy();
                if(proxy2 != null) {
                    logger.log(Level.DEBUG, CONFIGURING_PROXY_PATTERN, proxy2, url);
                } else {
                    logger.log(Level.DEBUG, NOT_CONFIGURING_PROXY_PATTERN, url);
                }
                return proxy2;
            default:
                logger.log(Level.ERROR, "Invalid protocol in URL '{}'. Must be either http or https", url);
                return null;
        }
    }

    public Authentication getAuthenticationForURL(URL url) {
        switch (url.getProtocol()) {
            case "http":
                Authentication auth = this.getHttpAuthentication();
                if(auth != null) {
                    logger.log(Level.DEBUG, "Using proxy authentication for url {}", url);
                } else {
                    logger.log(Level.DEBUG, "Using no proxy authentication for url {}", url);
                }
                return auth;
            case "https":
                Authentication auth2 = this.getHttpsAuthentication();
                if(auth2 != null) {
                    logger.log(Level.DEBUG, "Using proxy authentication for url {}", url);
                } else {
                    logger.log(Level.DEBUG, "Using no proxy authentication for url {}", url);
                }
                return auth2;
            default:
                logger.log(Level.ERROR, "URL has unknown scheme: {}", url);
                return null;
        }
    }
}
