package io.github.dschanoeh.healthbuddy.notifications.teams;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Validated
public class WebHookConfiguration {

    @Setter
    @URL
    @NotNull
    private String url;
    @Getter
    @Setter
    private String environmentPattern;

    public java.net.URL getUrl() {
        try {
            return new java.net.URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public Pattern getCompiledEnvironmentPattern() throws PatternSyntaxException {
        if (environmentPattern == null) {
            return null;
        }

        return Pattern.compile(environmentPattern);
    }
}
