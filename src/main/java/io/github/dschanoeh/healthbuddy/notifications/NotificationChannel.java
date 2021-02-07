package io.github.dschanoeh.healthbuddy.notifications;

import io.github.dschanoeh.healthbuddy.Incident;

public interface NotificationChannel {

    void openIncident(Incident i);
    void closeIncident(Incident i);
}
