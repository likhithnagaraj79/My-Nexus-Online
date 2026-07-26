package com.exhibitorreg.admin;

import com.exhibitorreg.admin.dto.CreateEventRequest;
import com.exhibitorreg.admin.dto.EventDayResponse;
import com.exhibitorreg.admin.dto.EventResponse;
import com.exhibitorreg.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEventService {

    private static final int AUTO_CREATED_DAY_COUNT = 3;

    private final EventRepository eventRepository;
    private final EventDayRepository eventDayRepository;

    public AdminEventService(EventRepository eventRepository, EventDayRepository eventDayRepository) {
        this.eventRepository = eventRepository;
        this.eventDayRepository = eventDayRepository;
    }

    /** Every event gets exactly Day 1/2/3 automatically — no calendar date, no separate Admin
     * step. The only consumer of a day's id (Validator check-in scans) never reads a date. */
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        eventRepository.save(event);

        for (int dayNumber = 1; dayNumber <= AUTO_CREATED_DAY_COUNT; dayNumber++) {
            EventDay day = new EventDay();
            day.setEvent(event);
            day.setDayNumber(dayNumber);
            eventDayRepository.save(day);
        }

        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents() {
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getActiveEvent() {
        return eventRepository.findByActiveTrue()
                .map(EventResponse::from)
                .orElseThrow(() -> new NotFoundException("No active event is configured."));
    }

    /** Activating an event auto-deactivates whichever one was previously active — at most one event is active at a time. */
    @Transactional
    public EventResponse activate(UUID eventId) {
        Event target = getOrThrow(eventId);

        eventRepository.findByActiveTrue()
                .filter(current -> !current.getId().equals(eventId))
                .ifPresent(current -> {
                    current.setActive(false);
                    eventRepository.save(current);
                });

        target.setActive(true);
        eventRepository.save(target);
        return EventResponse.from(target);
    }

    @Transactional
    public EventResponse deactivate(UUID eventId) {
        Event event = getOrThrow(eventId);
        event.setActive(false);
        eventRepository.save(event);
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventDayResponse> listEventDays(UUID eventId) {
        getOrThrow(eventId);
        return eventDayRepository.findByEventIdOrderByDayNumber(eventId).stream()
                .map(EventDayResponse::from)
                .toList();
    }

    private Event getOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    }
}
