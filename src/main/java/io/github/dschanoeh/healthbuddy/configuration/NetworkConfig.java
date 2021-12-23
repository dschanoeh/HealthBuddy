package io.github.dschanoeh.healthbuddy.configuration;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkConfig {
    private static final Logger logger = LogManager.getLogger(NetworkConfig.class);

    @Getter
    @Setter
    private String httpProxyHost;
    @Getter
    @Setter
    private Integer httpProxyPort;
    @Getter
    @Setter
    private Integer timeout = 5000;
    @Getter
    @Setter
    private Boolean followRedirects = false;

    public ProxyConfiguration getProxyConfiguration() {
        if(httpProxyHost != null && httpProxyPort != null) {
            logger.log(Level.INFO, "Reading proxy configuration from application config");
            return ProxyConfiguration.fromNetworkConfig(this);
        } else {
            if(System.getProperty("http.proxyHost") != null || System.getProperty("https.proxyHost") != null) {
                logger.log(Level.INFO, "Reading proxy configuration from system properties");
                return ProxyConfiguration.fromProperties();
            } else {
                logger.log(Level.INFO, "Reading proxy configuration from environment");
                return ProxyConfiguration.fromEnvironment();
            }
        }
    }
}
