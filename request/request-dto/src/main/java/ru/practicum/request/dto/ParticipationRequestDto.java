package ru.practicum.request.dto;

import lombok.Builder;

@Builder
public record ParticipationRequestDto(
        Long id,
        String created,
        Long event,
        Long requester,
        ParticipationStatus status
) {
}
