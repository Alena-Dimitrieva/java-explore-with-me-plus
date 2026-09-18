package ru.practicum.event.mapper;

import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;
import ru.practicum.event.dto.event.EventFullDto;
import ru.practicum.event.dto.event.EventShortDto;
import ru.practicum.event.dto.event.NewEventDto;
import ru.practicum.event.dto.event.UpdateEventAdminRequest;
import ru.practicum.event.dto.event.UpdateEventUserRequest;
import ru.practicum.event.model.Category;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.enums.EventState;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@UtilityClass
public class EventMapper {

	public EventShortDto toEventShortDto(
			@NonNull Event event,
			UserShortDto initiator,
			long confirmedRequests,
			long views) {

		return EventShortDto.builder()
				.annotation(event.getAnnotation())
				.category(
						CategoryMapper.toDto(
								event.getCategory()
						)
				)
				.confirmedRequests(confirmedRequests)
				.eventDate(event.getEventDate())
				.id(event.getId())
				.initiator(initiator)
				.paid(event.isPaid())
				.title(event.getTitle())
				.views(views)
				.rate(event.getRate())
				.build();
	}

	public EventFullDto toEventFullDto(
			@NonNull Event event,
			UserShortDto initiator,
			long confirmedRequests,
			long views) {

		return EventFullDto.builder()
				.id(event.getId())
				.annotation(event.getAnnotation())
				.category(
						CategoryMapper.toDto(
								event.getCategory()
						)
				)
				.confirmedRequests(confirmedRequests)
				.createdOn(event.getCreatedOn())
				.description(event.getDescription())
				.eventDate(event.getEventDate())
				.initiator(initiator)
				.location(event.getLocation())
				.paid(event.isPaid())
				.participantLimit(
						event.getParticipantLimit()
				)
				.publishedOn(event.getPublishedOn())
				.requestModeration(
						event.isRequestModeration()
				)
				.state(event.getState())
				.title(event.getTitle())
				.views(views)
				.rate(event.getRate())
				.build();
	}

	public Event toEntity(
			@NonNull NewEventDto dto,
			Category category,
			LocalDateTime createdOn,
			Long initiatorId,
			LocalDateTime publishedOn,
			EventState state) {

		return Event.builder()
				.annotation(dto.annotation())
				.category(category)
				.createdOn(createdOn)
				.description(dto.description())
				.eventDate(dto.eventDate())
				.initiatorId(initiatorId)
				.location(dto.location())
				.paid(
						dto.paid() != null
								&& dto.paid()
				)
				.participantLimit(
						dto.participantLimit() == null
								? 0
								: dto.participantLimit()
				)
				.publishedOn(publishedOn)
				.requestModeration(
						dto.requestModeration() == null
								|| dto.requestModeration()
				)
				.state(state)
				.title(dto.title())
				.rate(0)
				.build();
	}

	public Event update(
			@NonNull Event oldEvent,
			@NonNull UpdateEventAdminRequest request,
			EventState state,
			LocalDateTime publishedOn,
			Category category) {

		return oldEvent.toBuilder()
				.annotation(
						request.annotation() != null
								? request.annotation()
								: oldEvent.getAnnotation()
				)
				.category(
						category != null
								? category
								: oldEvent.getCategory()
				)
				.description(
						request.description() != null
								? request.description()
								: oldEvent.getDescription()
				)
				.eventDate(
						request.eventDate() != null
								? request.eventDate()
								: oldEvent.getEventDate()
				)
				.location(
						request.location() != null
								? request.location()
								: oldEvent.getLocation()
				)
				.paid(
						request.paid() != null
								? request.paid()
								: oldEvent.isPaid()
				)
				.participantLimit(
						request.participantLimit() != null
								? request.participantLimit()
								: oldEvent.getParticipantLimit()
				)
				.requestModeration(
						request.requestModeration() != null
								? request.requestModeration()
								: oldEvent.isRequestModeration()
				)
				.state(state)
				.title(
						request.title() != null
								? request.title()
								: oldEvent.getTitle()
				)
				.publishedOn(publishedOn)
				.build();
	}

	public void merge(
			@NonNull Event event,
			UpdateEventUserRequest request) {

		if (request == null) {
			return;
		}

		if (request.annotation() != null) {
			event.setAnnotation(request.annotation());
		}

		if (request.description() != null) {
			event.setDescription(request.description());
		}

		if (request.title() != null) {
			event.setTitle(request.title());
		}

		if (request.eventDate() != null) {
			event.setEventDate(request.eventDate());
		}

		if (request.location() != null) {
			event.setLocation(request.location());
		}

		if (request.paid() != null) {
			event.setPaid(request.paid());
		}

		if (request.participantLimit() != null) {
			event.setParticipantLimit(
					request.participantLimit()
			);
		}

		if (request.requestModeration() != null) {
			event.setRequestModeration(
					request.requestModeration()
			);
		}
	}
}