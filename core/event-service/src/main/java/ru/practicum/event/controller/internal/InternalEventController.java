package ru.practicum.event.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event.dao.EventRepository;

import ru.practicum.event.dto.EventInternalDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.enums.EventState;
import ru.practicum.event.util.error.exception.NotFoundException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{eventId}")
    public EventInternalDto getEvent(
            @PathVariable Long eventId) {

        Event event =
                eventRepository.findById(eventId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        "Событие с id="
                                                + eventId
                                                + " не найдено"
                                )
                        );

        return new EventInternalDto(
                event.getId(),
                event.getInitiatorId(),
                event.getState()
                        == EventState.PUBLISHED,
                event.getParticipantLimit(),
                event.isRequestModeration()
        );
    }

    @PatchMapping("/{eventId}/rate")
    public void updateRate(
            @PathVariable Long eventId,
            @RequestParam("rate") long rate) {

        Event event =
                eventRepository.findById(eventId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        "Событие с id="
                                                + eventId
                                                + " не найдено"
                                )
                        );

        event.setRate(rate);

        eventRepository.save(event);
    }
}