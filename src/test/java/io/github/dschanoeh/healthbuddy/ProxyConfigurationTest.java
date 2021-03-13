package io.github.dschanoeh.healthbuddy;

import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class ProxyConfigurationTest {

    @BeforeEach
    public void setUp() {
        System.setProperty("http.nonProxyHosts", "*foo.bar|*bar.baz");
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8080");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("http.nonProxyHosts");
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
    }

    @Test
    public void noProxyTest() throws MalformedURLException {
        ProxyConfiguration c = ProxyConfiguration.fromProperties();
        HttpHost proxyForURL = c.getProxyForURL(new URL("http://bla.foo.bar"));
        assert proxyForURL == null;

        proxyForURL = c.getProxyForURL(new URL("http://bla.foo2.bar"));
        assert proxyForURL != null;

        proxyForURL = c.getProxyForURL(new URL("http://foo.bar"));
        assert proxyForURL == null;
    }
}
