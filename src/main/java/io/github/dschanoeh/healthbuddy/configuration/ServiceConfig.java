package io.github.dschanoeh.healthbuddy.configuration;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
public class ServiceConfig {
    @NotNull
    @Getter
    @Setter
    @URL
    private String url;
    @NotNull
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private List<Integer> allowedStatusCodes;
    @Getter
    @Setter
    private List<String> allowedActuatorStatus;
    @Getter
    @Setter
    private String userName;
    @Getter
    @Setter
    private String password;
    @Getter
    @Setter
    private String environment;
}
