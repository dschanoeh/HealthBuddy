package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.configuration.ProxyConfiguration;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvironmentProxyConfigurationTest {

    private static final String HTTPS_PROXY_VALUE_WITH_AUTH = "https://foo:bar@baz.io:1234";
    private static final String HTTP_PROXY_VALUE_WITHOUT_AUTH = "http://baz.io:1234";
    private static final String HTTPS_URL = "https://bla.foo.bar";
    private static final String HTTP_URL = "http://bla.foo2.bar";


    @Test
    @ClearEnvironmentVariable(key = "HTTP_PROXY")
    @SetEnvironmentVariable(key = "HTTPS_PROXY", value = HTTPS_PROXY_VALUE_WITH_AUTH)
    void testHttpsProxy() throws MalformedURLException {
        ProxyConfiguration c = ProxyConfiguration.fromEnvironment();

        HttpHost proxyForURL = c.getProxyForURL(new URL(HTTPS_URL));
        ProxyConfiguration.Authentication authentication = c.getAuthenticationForURL(new URL(HTTPS_URL));
        assertNotNull(proxyForURL);
        assertNotNull(authentication);

        proxyForURL = c.getProxyForURL(new URL(HTTP_URL));
        authentication = c.getAuthenticationForURL(new URL(HTTP_URL));
        assertNull(proxyForURL);
        assertNull(authentication);
    }

    @Test
    @ClearEnvironmentVariable(key = "HTTPS_PROXY")
    @SetEnvironmentVariable(key = "HTTP_PROXY", value = HTTP_PROXY_VALUE_WITHOUT_AUTH)
    void testHttpProxyNoAuthentication() throws MalformedURLException {
        ProxyConfiguration c = ProxyConfiguration.fromEnvironment();

        HttpHost proxyForURL = c.getProxyForURL(new URL(HTTPS_URL));
        ProxyConfiguration.Authentication authentication = c.getAuthenticationForURL(new URL(HTTPS_URL));
        assertNull(proxyForURL);
        assertNull(authentication);

        proxyForURL = c.getProxyForURL(new URL(HTTP_URL));
        authentication = c.getAuthenticationForURL(new URL(HTTP_URL));
        assertNotNull(proxyForURL);
        assertNull(authentication);
    }

    @Test
    @ClearEnvironmentVariable(key = "HTTPS_PROXY")
    @ClearEnvironmentVariable(key = "HTTP_PROXY")
    void testNoProxyAtAll() throws MalformedURLException {
        ProxyConfiguration c = ProxyConfiguration.fromEnvironment();

        HttpHost proxyForURL = c.getProxyForURL(new URL(HTTPS_URL));
        ProxyConfiguration.Authentication authentication = c.getAuthenticationForURL(new URL(HTTPS_URL));
        assertNull(proxyForURL);
        assertNull(authentication);

        proxyForURL = c.getProxyForURL(new URL(HTTP_URL));
        authentication = c.getAuthenticationForURL(new URL(HTTP_URL));
        assertNull(proxyForURL);
        assertNull(authentication);
    }
}
