package ru.practicum.event.dto;

public record EventInternalDto(
        Long id,
        Long initiatorId,
        boolean published,
        int participantLimit,
        boolean requestModeration
) {
}
