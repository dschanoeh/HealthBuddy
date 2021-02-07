package io.github.dschanoeh.healthbuddy.notifications.teams;

public class TeamsConfiguration {
    private String webHookURL;
    public String getWebHookURL() {
        return webHookURL;
    }

    public void setWebHookURL(String webHookURL) {
        this.webHookURL = webHookURL;
    }
}
