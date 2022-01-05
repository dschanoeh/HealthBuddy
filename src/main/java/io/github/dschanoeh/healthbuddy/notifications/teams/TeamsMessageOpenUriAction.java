package io.github.dschanoeh.healthbuddy.notifications.teams;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Generated;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Generated
public class TeamsMessageOpenUriAction {
    @JsonProperty("@type")
    @Getter
    private final String type = "OpenUri";

    @Getter
    private final String name;

    @Getter
    private final List<Target> targets;

    private class Target {
        @Getter
        private final String os = "default";

        @Getter
        private final String uri;

        public Target(String uri) {
            this.uri = uri;
        }
    }

    public TeamsMessageOpenUriAction(String name, String uri) {
        this.name = name;
        this.targets = new ArrayList<>();
        targets.add(new Target(uri));
    }
}
