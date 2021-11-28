package io.github.dschanoeh.healthbuddy.notifications;

import io.github.dschanoeh.healthbuddy.Incident;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractNotificationReceiver {

    private Pattern environmentPattern;

    protected Pattern getEnvironmentPattern() {
        return this.environmentPattern;
    }

    protected void setEnvironmentPattern(Pattern pattern) {
        this.environmentPattern = pattern;
    }

    public Boolean shouldBeNotifiedAbout(Incident i) {
        if (environmentPattern != null) {
            if(i.getEnvironment() == null) {
                return false;
            }
            Matcher matcher = environmentPattern.matcher(i.getEnvironment());
            return matcher.matches();
        }
        return true;
    }
}
