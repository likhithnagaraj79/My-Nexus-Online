package com.exhibitorreg.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exhibitorreg.admin.dto.CreateEventDayRequest;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.NotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventDayRepository eventDayRepository;

    private AdminEventService service;

    @BeforeEach
    void setUp() {
        service = new AdminEventService(eventRepository, eventDayRepository);
    }

    private static Event eventWithId(boolean active) {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        event.setName("Expo");
        event.setStartDate(LocalDate.of(2026, 8, 1));
        event.setEndDate(LocalDate.of(2026, 8, 3));
        event.setActive(active);
        return event;
    }

    @Test
    void activatingAnEventDeactivatesThePreviouslyActiveOne() {
        Event previouslyActive = eventWithId(true);
        Event target = eventWithId(false);
        when(eventRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.of(previouslyActive));

        service.activate(target.getId());

        assertThat(previouslyActive.isActive()).isFalse();
        assertThat(target.isActive()).isTrue();
    }

    @Test
    void activatingTheAlreadyActiveEventIsANoOp() {
        Event target = eventWithId(true);
        when(eventRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.of(target));

        service.activate(target.getId());

        assertThat(target.isActive()).isTrue();
    }

    @Test
    void activatingWhenNoneIsCurrentlyActiveJustActivatesTarget() {
        Event target = eventWithId(false);
        when(eventRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(eventRepository.findByActiveTrue()).thenReturn(Optional.empty());

        service.activate(target.getId());

        assertThat(target.isActive()).isTrue();
    }

    @Test
    void activateUnknownEventThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(id)).isInstanceOf(NotFoundException.class);
        verify(eventRepository, never()).findByActiveTrue();
    }

    @Test
    void deactivateSetsFlagFalse() {
        Event event = eventWithId(true);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        service.deactivate(event.getId());

        assertThat(event.isActive()).isFalse();
    }

    @Test
    void eventDayOutsideEventRangeIsRejected() {
        Event event = eventWithId(true);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.createEventDay(
                        event.getId(), new CreateEventDayRequest(1, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void eventDayWithinRangeIsAccepted() {
        Event event = eventWithId(true);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        var response = service.createEventDay(event.getId(), new CreateEventDayRequest(1, LocalDate.of(2026, 8, 2)));

        assertThat(response.dayNumber()).isEqualTo(1);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 8, 2));
    }
}
