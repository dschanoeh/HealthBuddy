package io.github.dschanoeh.healthbuddy.notifications.teams;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class TeamsMessageSection {
    @AllArgsConstructor
    public static class Fact {
        @Getter
        @Setter
        private String name;
        @Getter
        @Setter
        private String value;
    }
    @Getter
    @Setter
    private String activityTitle;
    @Getter
    @Setter
    private String activitySubtitle;
    @Getter
    @Setter
    private String activityImage;
    @Getter
    @Setter
    private List<Fact> facts = new ArrayList<Fact>();
    @JsonProperty("markdown")
    private final Boolean markdown = true;
}
