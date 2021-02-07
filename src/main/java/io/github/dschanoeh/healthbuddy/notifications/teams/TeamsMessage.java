package io.github.dschanoeh.healthbuddy.notifications.teams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamsMessage {
    public static final String COLOR_RED = "FF0000";
    public static final String COLOR_GREEN = "00FF00";
    private String title;
    private String text;
    private String themeColor;

    @JsonProperty("@context")
    private final String context = "https://schema.org/extensions";

    @JsonProperty("@type")
    private final String type = "MessageCard";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }
}

