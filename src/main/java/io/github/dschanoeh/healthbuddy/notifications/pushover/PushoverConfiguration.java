package io.github.dschanoeh.healthbuddy.notifications.pushover;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
@Configuration
public class PushoverConfiguration  {

    @Getter
    @Setter
    @NotNull(message = "An application token must be provided")
    private String applicationToken;

    @Getter
    @Setter
    @NotNull(message = "At least one recipient must be configured")
    private List<RecipientConfiguration> recipients;
}
