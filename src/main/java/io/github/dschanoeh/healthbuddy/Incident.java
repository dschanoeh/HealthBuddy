package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

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
    private final NotificationChannel channel;
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

    public Incident(Type type, NotificationChannel channel) {
        this.type = type;
        this.channel = channel;
    }

    public void open() {
        this.startDate = ZonedDateTime.now();
        this.state = State.ACTIVE;
        channel.openIncident(this);
    }

    public void close() {
        this.endDate = ZonedDateTime.now();
        this.state = State.RESOLVED;
        channel.closeIncident(this);
    }

    public Boolean isOpen() {
        return state == State.ACTIVE;
    }
}
