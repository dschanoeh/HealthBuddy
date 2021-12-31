package io.github.dschanoeh.healthbuddy.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import java.net.URL;

@Validated
public class DashboardConfiguration {

    @Getter
    @Setter
    @Min(1L)
    private Long historyWindowDuration = 24 * 60L;

    @Getter
    @Setter
    private URL basePath;
}
