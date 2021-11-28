package io.github.dschanoeh.healthbuddy.notifications;

import io.github.dschanoeh.healthbuddy.notifications.pushover.PushoverConfiguration;
import io.github.dschanoeh.healthbuddy.notifications.teams.TeamsConfiguration;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Validated
@Generated
public class NotificationServiceConfiguration {
    @NestedConfigurationProperty
    @Getter
    @Setter
    private TeamsConfiguration teams;

    @NestedConfigurationProperty
    @Getter
    @Setter
    private PushoverConfiguration pushover;
}
