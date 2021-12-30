package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class Incident {
    public enum Type {UNEXPECTED_RESPONSE, NOT_REACHABLE}
    public enum State { ACTIVE, RESOLVED }

    @Getter
    private ZonedDateTime startDate;
    @Getter
    private ZonedDateTime endDate;
    @Getter
    private final Type type;
    private State state;
    @Getter
    @Setter
    private String serviceName;
    private final List<NotificationChannel> channels;
    @Getter
    @Setter
    private String body;
    @Getter
    @Setter
    private Integer httpStatus;
    @Getter
    @Setter
    private String environment;
    @Getter
    @Setter
    private String url;
    @Getter
    @Setter
    private UUID serviceId;

    public Incident(Type type, List<NotificationChannel> channels) {
        this.type = type;
        this.channels = channels;
    }

    public void open() {
        this.startDate = ZonedDateTime.now();
        this.state = State.ACTIVE;
        for(NotificationChannel c : channels) {
            c.openIncident(this);
        }
    }

    public void close() {
        this.endDate = ZonedDateTime.now();
        this.state = State.RESOLVED;
        for(NotificationChannel c : channels) {
            c.closeIncident(this);
        }
    }

    public Boolean isOpen() {
        return state == State.ACTIVE;
    }
}
