package ru.practicum.event.dto.event;

import lombok.Builder;
import ru.practicum.event.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminGetDto(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Integer from,
        Integer size
) {
}