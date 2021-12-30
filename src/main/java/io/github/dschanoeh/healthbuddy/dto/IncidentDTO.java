package io.github.dschanoeh.healthbuddy.dto;

import io.github.dschanoeh.healthbuddy.Incident;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
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
