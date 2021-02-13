package io.github.dschanoeh.healthbuddy;

import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public class ServiceConfig {
    @NonNull
    private String url;
    @NonNull
    private Integer checkInterval;
    @NonNull
    private String name;
    @NonNull
    private List<Integer> allowedStatusCodes;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Integer checkInterval) {
        this.checkInterval = checkInterval;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getAllowedStatusCodes() {
        return allowedStatusCodes;
    }

    public void setAllowedStatusCodes(List<Integer> allowedStatusCodes) {
        this.allowedStatusCodes = allowedStatusCodes;
    }
}
