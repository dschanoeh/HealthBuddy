package io.github.dschanoeh.healthbuddy;

import io.github.dschanoeh.healthbuddy.notifications.NotificationChannel;

import java.util.Date;

public class Incident {
    public static enum Type {UNEXPECTED_RESPONSE, NOT_REACHABLE};
    public static enum State { ACTIVE, RESOLVED }

    private Date startDate;
    private Date endDate;
    private Type type;
    private State state;
    private String serviceName;
    private NotificationChannel channel;
    private String body;
    private Integer httpStatus;


    public Incident(Type type, NotificationChannel channel) {
        this.type = type;
        this.channel = channel;
    }

    public void open() {
        this.startDate = new Date();
        this.state = State.ACTIVE;
        channel.openIncident(this);
    }

    public void close() {
        this.endDate = new Date();
        this.state = State.RESOLVED;
        channel.closeIncident(this);
    }

    public Boolean isOpen() {
        return state == State.ACTIVE;
    }

    public State getState() {
        return state;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Type getType() {
        return type;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
