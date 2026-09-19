package ru.practicum.event.dto.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record FreeGetDto(
        String text,
        List<Long> categories,
        Boolean paid,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Boolean onlyAvailable,
        FreeEventSort sort,
        Integer from,
        Integer size
) {

    public enum FreeEventSort {
        EVENT_DATE,
        VIEWS
    }
}
