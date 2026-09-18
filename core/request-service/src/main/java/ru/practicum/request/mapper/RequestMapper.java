package ru.practicum.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class RequestMapper {

	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofPattern(
					"yyyy-MM-dd'T'HH:mm:ss.SSS"
			);

	public ParticipationRequestDto
	toParticipationRequestDto(
			ParticipationRequest request) {

		if (request == null) {
			return null;
		}

		return ParticipationRequestDto.builder()
				.event(request.getEventId())
				.requester(request.getRequesterId())
				.status(request.getStatus())
				.created(
						formatDateTime(
								request.getCreated()
						)
				)
				.id(request.getId())
				.build();
	}

	private String formatDateTime(
			LocalDateTime dateTime) {

		if (dateTime == null) {
			return null;
		}

		return dateTime.format(FORMATTER);
	}
}