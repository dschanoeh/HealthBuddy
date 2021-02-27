package io.github.dschanoeh.healthbuddy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

public class ActuatorHealthResponse {
    @Getter
    @Setter
    private String status;
    @Getter
    @Setter
    private JsonNode details;
}
