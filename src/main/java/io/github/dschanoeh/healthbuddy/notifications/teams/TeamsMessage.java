package io.github.dschanoeh.healthbuddy.notifications.teams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated
public class TeamsMessage {
    public static final String COLOR_RED = "FF0000";
    public static final String COLOR_GREEN = "00FF00";
    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private String summary;
    @Getter
    @Setter
    private String themeColor;
    @Getter
    @Setter
    private List<TeamsMessageSection> sections = new ArrayList<>();

    @JsonProperty("@context")
    private final String context = "https://schema.org/extensions";

    @JsonProperty("@type")
    private final String type = "MessageCard";

    @JsonProperty("potentialAction")
    private final List<TeamsMessageOpenUriAction> actions = new ArrayList<>();

    public void addAction(TeamsMessageOpenUriAction action) {
        this.actions.add(action);
    }
}

