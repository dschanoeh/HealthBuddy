package io.github.dschanoeh.healthbuddy.notifications;


import io.github.dschanoeh.healthbuddy.Incident;
import io.github.dschanoeh.healthbuddy.dto.IncidentDTO;
import io.github.dschanoeh.healthbuddy.dto.IncidentHistoryDTO;
import io.github.dschanoeh.healthbuddy.dto.IncidentHistoryEntryDTO;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class IncidentHistoryCollector implements NotificationChannel {
    private static final Logger logger = LogManager.getLogger(NotificationChannel.class);

    private static final Long HISTORY_WINDOW_IN_MINUTES = 1 * 60L;

    private final ZonedDateTime serviceStartTime = ZonedDateTime.now();
    private final Map<UUID, List<Incident>> incidentHistory = new HashMap<>();

    @Override
    public void openIncident(Incident i) {
        logger.log(Level.DEBUG, "Adding new incident for service {} to the history", i.getServiceName());
        UUID serviceId = i.getServiceId();

        if(incidentHistory.containsKey(serviceId)) {
            List<Incident> incidentList = incidentHistory.get(serviceId);
            incidentList.add(i);
        } else {
            List<Incident> incidentList = new ArrayList<>();
            incidentList.add(i);
            incidentHistory.put(serviceId, incidentList);
        }
    }

    @Override
    public void closeIncident(Incident i) {
    }

    public List<Incident> getHistoryForService(UUID id) {
        return incidentHistory.get(id);
    }

    public IncidentHistoryDTO getIncidentHistory(UUID id) {
        List<Incident> incidentList = getHistoryForService(id);
        List<IncidentHistoryEntryDTO> history = new ArrayList<>();
        IncidentHistoryDTO historyDTO = new IncidentHistoryDTO();
        historyDTO.setHistoryMaximum(HISTORY_WINDOW_IN_MINUTES);

        // If the service was started within the history window, fill time with an "UNKNOWN" block
        if(ChronoUnit.MINUTES.between(serviceStartTime, ZonedDateTime.now()) < HISTORY_WINDOW_IN_MINUTES) {
            IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
            entry.setStatus(IncidentHistoryEntryDTO.Status.UNKNOWN);
            entry.setStart(0L);
            entry.setEnd(minutesFromStartOfWindowTill(serviceStartTime));
            history.add(entry);
        }

        // If there are no incidents recorded, we need to just add an 'UP' entry and are done
        if(incidentList == null || incidentList.isEmpty()) {
            IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
            entry.setStatus(IncidentHistoryEntryDTO.Status.UP);
            entry.setStart(minutesFromStartOfWindowTill(serviceStartTime));
            entry.setEnd(HISTORY_WINDOW_IN_MINUTES);
            history.add(entry);

            historyDTO.setHistory(history);
            historyDTO.setHistoryMaximum(HISTORY_WINDOW_IN_MINUTES);
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
                    && incident.getStartDate().isAfter(serviceStartTime)
                    && ChronoUnit.MINUTES.between(serviceStartTime, incident.getStartDate()) > 0) {
                IncidentHistoryEntryDTO entry = new IncidentHistoryEntryDTO();
                entry.setStatus(IncidentHistoryEntryDTO.Status.UP);
                entry.setStart(minutesFromStartOfWindowTill(serviceStartTime));
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
            if(incident.isOpen()) {
                entry.setEnd(HISTORY_WINDOW_IN_MINUTES);
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
            if(i == incidentList.size()-1 && !incidentList.get(i).isOpen()) {
                IncidentHistoryEntryDTO finalEntry = new IncidentHistoryEntryDTO();
                finalEntry.setStatus(IncidentHistoryEntryDTO.Status.UP);
                finalEntry.setStart(minutesFromStartOfWindowTill(incident.getEndDate()));
                finalEntry.setEnd(HISTORY_WINDOW_IN_MINUTES);
                history.add(finalEntry);
            }
        }

        historyDTO.setHistory(history);
        return historyDTO;
    }

    private static Long minutesFromStartOfWindowTill(ZonedDateTime time) {
        ZonedDateTime startOfWindow = ZonedDateTime.now().minus(HISTORY_WINDOW_IN_MINUTES, ChronoUnit.MINUTES);
        return ChronoUnit.MINUTES.between(startOfWindow, time);
    }
}
