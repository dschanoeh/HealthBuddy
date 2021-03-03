package io.github.dschanoeh.healthbuddy.notifications.teams;

import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.validation.annotation.Validated;

import java.net.MalformedURLException;

@Validated
public class TeamsConfiguration {
    @Setter
    @URL
    private String webHookURL;

    public java.net.URL getWebHookURL() {
        try {
            java.net.URL url = new java.net.URL(webHookURL);
            return url;
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
