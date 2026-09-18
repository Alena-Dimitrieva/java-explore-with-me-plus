package ru.practicum.event.dto.event;

import lombok.Builder;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;


import java.time.LocalDateTime;

@Builder
public record EventShortDto(
		long id,
		String annotation,
		CategoryDto category,
		long confirmedRequests,
		LocalDateTime eventDate,
		UserShortDto initiator,
		boolean paid,
		String title,
		long views,
		long rate
) {
}
