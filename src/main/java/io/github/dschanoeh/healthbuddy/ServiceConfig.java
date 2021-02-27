package io.github.dschanoeh.healthbuddy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public class ServiceConfig {
    @NonNull
    @Getter
    @Setter
    private String url;
    @NonNull
    @Getter
    @Setter
    private Integer checkInterval;
    @NonNull
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
