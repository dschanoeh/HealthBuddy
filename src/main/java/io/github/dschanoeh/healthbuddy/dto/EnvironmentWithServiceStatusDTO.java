package io.github.dschanoeh.healthbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentWithServiceStatusDTO {

    @Getter
    @Setter
    private String environmentName;
    @Getter
    @Setter
    private Integer servicesUpCount = 0;
    @Getter
    @Setter
    private Integer servicesCount = 0;
    @Getter
    @Setter
    private List<ServiceStatusDTO> services = new ArrayList<>();
}
