package io.github.dschanoeh.healthbuddy.configuration;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Validated
@Configuration
public class ReferenceEndpointConfiguration {
    @NotNull
    @Getter
    @Setter
    @URL
    private String url;

}
