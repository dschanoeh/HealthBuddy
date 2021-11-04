package io.github.dschanoeh.healthbuddy.notifications.teams;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public class TeamsConfiguration {

    @Getter
    @Setter
    List<WebHookConfiguration> hooks;

}
