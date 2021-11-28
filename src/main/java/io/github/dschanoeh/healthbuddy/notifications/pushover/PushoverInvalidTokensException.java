package io.github.dschanoeh.healthbuddy.notifications.pushover;

import lombok.Generated;

@Generated
public class PushoverInvalidTokensException extends Exception {

    @Override
    public String getMessage() {
        return "The provided pushover application and/or user token could not be validated";
    }
}
