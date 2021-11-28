package io.github.dschanoeh.healthbuddy.notifications.pushover;

import de.svenkubiak.jpushover.enums.Priority;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Validated
@Configuration
@Generated
public class RecipientConfiguration {

    @Getter
    @Setter
    @NotNull(message = "A token must be provided for each recipient")
    private String token;

    @Getter
    @Setter
    private String environmentPattern;

    @Getter
    @Setter
    @NotNull
    private Priority priority = Priority.NORMAL;

    public Pattern getCompiledEnvironmentPattern() throws PatternSyntaxException {
        if (environmentPattern == null) {
            return null;
        }

        return Pattern.compile(environmentPattern);
    }
}
