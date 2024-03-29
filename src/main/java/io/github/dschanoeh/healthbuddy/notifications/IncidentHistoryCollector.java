package io.github.dschanoeh.healthbuddy.notifications;


import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.configuration.HealthBuddyConfiguration;
import io.github.dschanoeh.healthbuddy.dto.IncidentDTO;
import io.github.dschanoeh.healthbuddy.dto.IncidentHistoryDTO;
import io.github.dschanoeh.healthbuddy.dto.IncidentHistoryEntryDTO;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IncidentHistoryCollector implements NotificationChannel {
    private static final Logger logger = LogManager.getLogger(IncidentHistoryCollector.class);

    private HealthBuddyConfiguration configuration;

    private final ZonedDateTime serviceStartTime = getCurrentTime();
    private final Map<UUID, List<Incident>> incidentHistory = new ConcurrentHashMap<>();

    @Autowired @Lazy
    public void setConfiguration(HealthBuddyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void openIncident(Incident i) {
        logger.log(Level.DEBUG, "Adding new incident for service {} to the history", i.getServiceName());
        UUID serviceId = i.getServiceId();

        if(incidentHistory.containsKey(serviceId)) {
            List<Incident> incidentList = incidentHistory.get(serviceId);
            incidentList.add(i);
        } else {
            List<Incident> incidentList = Collections.synchronizedList(new ArrayList<>());
            incidentList.add(i);
            incidentHistory.put(serviceId, incidentList);
        }
    }

    @Override
    public void closeIncident(Incident i) {
    }

    public List<Incident> getHistoryForService(UUID id) {
        List<Incident> history = incidentHistory.get(id);
        if (history == null) {
            return null;
        }

        history.removeIf(i -> !i.isOpen() && minutesFromStartOfWindowTill(i.getEndDate()) < 0);
        return history;
    }

    private Long getHistoryWindowDuration() {
        return configuration.getDashboard().getHistoryWindowDuration();
    }

    public IncidentHistoryDTO getIncidentHistory(UUID id) {
        List<Incident> incidentList = getHistoryForService(id);
        List<IncidentHistoryEntryDTO> history = new ArrayList<>();
        IncidentHistoryDTO historyDTO = new IncidentHistoryDTO();
        historyDTO.setHistoryMaximum(getHistoryWindowDuration());

        // If the service was started within the history window, fill time with an "UNKNOWN" block
        if(ChronoUnit.MINUTES.between(getServiceStartTime(), getCurrentTime()) < getHistoryWindowDuration()) {
            IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
            entry.setStatus(IncidentHistoryEntryDTO.Status.UNKNOWN);
            entry.setStart(0L);
            entry.setEnd(minutesFromStartOfWindowTill(getServiceStartTime()));
            history.add(entry);
        }

        // If there are no incidents recorded, we need to just add an 'UP' entry and are done
        if(incidentList == null || incidentList.isEmpty()) {
            IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
            entry.setStatus(IncidentHistoryEntryDTO.Status.UP);
            entry.setStart(serviceStartTimeFromStartOfWindow());
            entry.setEnd(getHistoryWindowDuration());
            history.add(entry);

            historyDTO.setHistory(history);
            historyDTO.setHistoryMaximum(getHistoryWindowDuration());
            return historyDTO;
        }

        // Otherwise, we sort the incidents and process them one by one
        incidentList.sort(Comparator.comparing(Incident::getStartDate));
        for(Integer i=0;i<incidentList.size();i++) {
            Incident incident = incidentList.get(i);

            /* If this is the first incident and there is a gap between start time and the incident,
                we need to add an 'UP' entry first.
             */
            if(i==0
                    && incident.getStartDate().isAfter(getServiceStartTime())
                    && ChronoUnit.MINUTES.between(getServiceStartTime(), incident.getStartDate()) > 0) {
                IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
                entry.setStatus(IncidentHistoryEntryDTO.Status.UP);
                entry.setStart(serviceStartTimeFromStartOfWindow());
                entry.setEnd(minutesFromStartOfWindowTill(incident.getStartDate()));
                history.add(entry);
            }

            /* If this is a later entry, we need to add an 'UP' gap between this
                and the previous entry.
             */
            if(i > 0) {
                Incident previousIncident = incidentList.get(i-1);
                IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
                entry.setStatus(IncidentHistoryEntryDTO.Status.UP);
                entry.setStart(minutesFromStartOfWindowTill(previousIncident.getEndDate()));
                entry.setEnd(minutesFromStartOfWindowTill(incident.getStartDate()));
                history.add(entry);
            }

            IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
            entry.setStatus(IncidentHistoryEntryDTO.Status.DOWN);
            entry.setStart(minutesFromStartOfWindowTill(incident.getStartDate()));
            if(Boolean.TRUE.equals(incident.isOpen())) {
                entry.setEnd(getHistoryWindowDuration());
            } else {
                entry.setEnd(minutesFromStartOfWindowTill(incident.getEndDate()));
            }
            IncidentDTO incidentDTO = new IncidentDTO();
            incidentDTO.setType(incident.getType());
            incidentDTO.setBody(incident.getBody());
            incidentDTO.setStatusCode(incident.getHttpStatus());
            incidentDTO.setStartDate(incident.getStartDate());
            incidentDTO.setEndDate(incident.getEndDate());
            entry.setIncident(incidentDTO);
            history.add(entry);

            /* And finally, if the last incident is not open anymore, we also need to add an 'UP' entry in the end */
            if(i == incidentList.size()-1 && Boolean.FALSE.equals(incidentList.get(i).isOpen())) {
                IncidentHistoryEntryDTO finalEntry = new IncidentHistoryEntryDTO();
                finalEntry.setStatus(IncidentHistoryEntryDTO.Status.UP);
                finalEntry.setStart(minutesFromStartOfWindowTill(incident.getEndDate()));
                finalEntry.setEnd(getHistoryWindowDuration());
                history.add(finalEntry);
            }
        }

        historyDTO.setHistory(history);
        return historyDTO;
    }

    /** Returns the service start time in minutes from the beginning of the window or '0' in case the service
     * started before the window.
     */
    private Long serviceStartTimeFromStartOfWindow() {
        Long minutes = minutesFromStartOfWindowTill(getServiceStartTime());
        return minutes > 0 ? minutes : 0;
    }

    /** Converts a time stamp to minutes from the beginning of the window.
     */
    private Long minutesFromStartOfWindowTill(ZonedDateTime time) {
        ZonedDateTime startOfWindow = getCurrentTime().minus(getHistoryWindowDuration(), ChronoUnit.MINUTES);
        return ChronoUnit.MINUTES.between(startOfWindow, time);
    }

    protected ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now();
    }

    protected ZonedDateTime getServiceStartTime() {
        return serviceStartTime;
    }

    protected void clearHistory() {
        incidentHistory.clear();
    }
}
