package io.github.dschanoeh.healthbuddy.dto;

import io.github.dschanoeh.healthbuddy.Incident;
import lombok.*;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Generated
public class IncidentDTO {

    @Getter
    @Setter
    Incident.Type type;

    @Getter
    @Setter
    private ZonedDateTime startDate;

    @Getter
    @Setter
    private ZonedDateTime endDate;

    @Getter
    @Setter
    private Integer statusCode;

    @Getter
    @Setter
    private String body;
}
