package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.request.dto.EventRequestCountDto;

import java.util.List;

/**
 * Клиент для межсервисного взаимодействия с request-service.
 * Используется main-service (временно) и event-service —
 * чтобы получать количество подтверждённых заявок на события.
 */
@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestClient {

    @GetMapping("/{eventId}/confirmed-count")
    long getConfirmedCount(@PathVariable("eventId") Long eventId);

    /**
     * Чтобы не создавать N+1 при обработке списков событий.
     */
    @GetMapping("/confirmed-count")
    List<EventRequestCountDto> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);
}