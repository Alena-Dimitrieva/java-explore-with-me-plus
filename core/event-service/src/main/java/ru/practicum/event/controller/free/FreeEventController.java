package ru.practicum.event.controller.free;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.event.EventFullDto;
import ru.practicum.event.dto.event.EventShortDto;
import ru.practicum.event.dto.event.FreeGetDto;
import ru.practicum.event.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class FreeEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getFreeEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false)
            FreeGetDto.FreeEventSort sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        FreeGetDto dto = FreeGetDto.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        return eventService.getFreeEvents(dto);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") long userId) {

        return eventService.getRecommendedEvents(userId);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getFreeEventById(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") long userId) {

        return eventService.getFreeEventById(eventId, userId);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") long userId) {

        eventService.likeEvent(userId, eventId);
    }
}