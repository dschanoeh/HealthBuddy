package io.github.dschanoeh.healthbuddy.dto;


import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Generated
public class IncidentHistoryEntryDTO {

    public enum Status {UNKNOWN, UP, DOWN}

    @Getter
    @Setter
    private Status status;
    @Getter
    @Setter
    private IncidentDTO incident;
    @Getter
    @Setter
    private Long start;
    @Getter
    @Setter
    private Long end;

    @Getter
    private final UUID id = UUID.randomUUID();
}
