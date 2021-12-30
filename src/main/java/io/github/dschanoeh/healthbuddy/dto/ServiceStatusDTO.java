package io.github.dschanoeh.healthbuddy.dto;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@Generated
@NoArgsConstructor
@Builder
public class ServiceStatusDTO {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private UUID id;

    @Getter
    @Setter
    private String url;

    @Getter
    @Setter
    private String environment;

    @Getter
    @Setter
    private Boolean isUp;

    @Getter
    @Setter
    private IncidentDTO currentIncident;
}
