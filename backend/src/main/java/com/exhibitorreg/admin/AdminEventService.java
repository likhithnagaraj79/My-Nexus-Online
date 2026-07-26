package com.exhibitorreg.admin;

import com.exhibitorreg.admin.dto.CreateEventDayRequest;
import com.exhibitorreg.admin.dto.CreateEventRequest;
import com.exhibitorreg.admin.dto.EventDayResponse;
import com.exhibitorreg.admin.dto.EventResponse;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEventService {

    private final EventRepository eventRepository;
    private final EventDayRepository eventDayRepository;

    public AdminEventService(EventRepository eventRepository, EventDayRepository eventDayRepository) {
        this.eventRepository = eventRepository;
        this.eventDayRepository = eventDayRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        eventRepository.save(event);
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

    @Transactional
    public EventDayResponse createEventDay(UUID eventId, CreateEventDayRequest request) {
        Event event = getOrThrow(eventId);

        if (request.date().isBefore(event.getStartDate()) || request.date().isAfter(event.getEndDate())) {
            throw new BusinessRuleViolationException("Event day date must fall within the event's start and end dates.");
        }

        EventDay day = new EventDay();
        day.setEvent(event);
        day.setDayNumber(request.dayNumber());
        day.setDate(request.date());
        eventDayRepository.saveAndFlush(day);
        return EventDayResponse.from(day);
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
