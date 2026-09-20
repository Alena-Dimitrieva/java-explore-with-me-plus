package ru.practicum.event.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.model.Location;
import ru.practicum.event.model.enums.EventState;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Builder
public record EventFullDto(

        Long id,

        @NotBlank
        String annotation,

        @NotNull
        CategoryDto category,

        Long confirmedRequests,

        LocalDateTime createdOn,

        String description,

        @NotNull
        LocalDateTime eventDate,

        @NotNull
        UserShortDto initiator,

        @NotNull
        Location location,

        boolean paid,

        Integer participantLimit,

        LocalDateTime publishedOn,

        boolean requestModeration,

        EventState state,

        @NotBlank
        String title,

        Long views,

        long rate
) {
}