package ru.practicum.request.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dao.RequestRepository;
import ru.practicum.request.dto.EventRequestCountDto;
import ru.practicum.request.dto.ParticipationStatus;
import ru.practicum.request.service.EventRequestCount;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestRepository requestRepository;

    @GetMapping("/{eventId}/confirmed-count")
    public long getConfirmedCount(@PathVariable Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);
    }

    @GetMapping("/confirmed-count")
    public List<EventRequestCountDto> getConfirmedCounts(@RequestParam List<Long> eventIds) {
        List<EventRequestCount> counts = requestRepository.countConfirmedRequestsByEventIds(
                eventIds, ParticipationStatus.CONFIRMED);
        return counts.stream()
                .map(c -> new EventRequestCountDto(c.getEventId(), c.getCount()))
                .toList();
    }
}
