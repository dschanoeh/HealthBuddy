package io.github.dschanoeh.healthbuddy.controller;

import io.github.dschanoeh.healthbuddy.ServiceMonitor;
import io.github.dschanoeh.healthbuddy.dto.EnvironmentWithServiceStatusDTO;
import io.github.dschanoeh.healthbuddy.dto.ServiceStatusDTO;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/services")
@AllArgsConstructor
public class ServicesController {
    private static final Logger logger = LogManager.getLogger(ServicesController.class);

    private final ServiceMonitor serviceMonitor;

    @GetMapping("")
    public List<EnvironmentWithServiceStatusDTO> getServiceStatus() {
        List<ServiceStatusDTO> serviceStatus = serviceMonitor.getServiceStatus();
        Map<String,EnvironmentWithServiceStatusDTO> environments = new HashMap<>();
        for(ServiceStatusDTO service : serviceStatus) {
            String environmentName = service.getEnvironment();
            // Services without an environment explicitly set will be grouped under an empty string environment
            if(environmentName == null) {
                environmentName = "";
            }
            EnvironmentWithServiceStatusDTO environment = null;
            if(environments.containsKey(environmentName)) {
                environment = environments.get(environmentName);
            } else {
                environment = new EnvironmentWithServiceStatusDTO();
                environment.setEnvironmentName(environmentName);
                environments.put(environmentName, environment);
            }
            environment.getServices().add(service);
            environment.setServicesCount(environment.getServicesCount()+1);
            if(service.getIsUp()) {
                environment.setServicesUpCount(environment.getServicesUpCount()+1);
            }
        }
        List<EnvironmentWithServiceStatusDTO> responseList =
                new ArrayList<EnvironmentWithServiceStatusDTO>(environments.values());

        // Sort the environments and the services within the environment
        responseList.sort(Comparator
                .comparing(EnvironmentWithServiceStatusDTO::getEnvironmentName));
        for(EnvironmentWithServiceStatusDTO environment : responseList) {
            environment.getServices().sort(Comparator.comparing(ServiceStatusDTO::getName));
        }

        return responseList;
    }

    @GetMapping("/{id}")
    public ServiceStatusDTO getServiceStatusByID(@PathVariable("id") UUID id) {
        logger.log(Level.INFO, "Returning service status by ID for {}", id);
        return serviceMonitor.getServiceStatus(id);
    }
}
