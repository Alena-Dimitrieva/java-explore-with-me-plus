package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "event-service",
        path = "/internal/events"
)
public interface EventClient {

    @GetMapping("/{eventId}")
    ru.practicum.event.dto.EventInternalDto getEvent(
            @PathVariable("eventId") Long eventId
    );

    @PatchMapping("/{eventId}/rate")
    void updateRate(
            @PathVariable("eventId") Long eventId,
            @RequestParam("rate") long rate
    );
}
