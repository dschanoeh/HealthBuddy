package io.github.dschanoeh.healthbuddy.dto;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Generated
public class IncidentHistoryDTO {
    @Getter
    @Setter
    private List<IncidentHistoryEntryDTO> history;

    @Getter
    @Setter
    private Long historyMaximum;
}
