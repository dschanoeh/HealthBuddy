package io.github.dschanoeh.healthbuddy;

import lombok.Getter;
import lombok.Setter;

public class NetworkConfig {
    @Getter
    @Setter
    private String httpProxyHost;
    @Getter
    @Setter
    private Integer httpProxyPort;
    @Getter
    @Setter
    private Integer timeout = 5000;
}
