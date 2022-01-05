package io.github.dschanoeh.healthbuddy.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Generated
public class ActuatorHealthResponseDTO {
    @Getter
    @Setter
    private String status;
    @Getter
    @Setter
    private JsonNode details;
}
