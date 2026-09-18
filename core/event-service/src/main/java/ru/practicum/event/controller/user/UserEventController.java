package ru.practicum.event.controller.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.practicum.event.dto.event.EventFullDto;
import ru.practicum.event.dto.event.EventShortDto;
import ru.practicum.event.dto.event.NewEventDto;

import ru.practicum.event.dto.event.UpdateEventUserRequest;
import ru.practicum.event.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
public class UserEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> findEventsByUserId(@PathVariable @Positive Long userId,
                                                  @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                  @RequestParam(defaultValue = "10") @Positive Integer size) {
        return eventService.findByUserId(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findEventById(@PathVariable @Positive Long userId,
                                      @PathVariable @Positive Long eventId) {
        return eventService.findEventById(userId, eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable @Positive Long userId,
                                 @RequestBody @Valid NewEventDto newEventDto) {
        return eventService.userAddNewEvent(userId, newEventDto);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto patchEvent(@PathVariable @Positive Long userId,
                                   @PathVariable @Positive Long eventId,
                                   @RequestBody @Valid UpdateEventUserRequest request) {
        return eventService.patchEvent(userId, eventId, request);
    }
}
