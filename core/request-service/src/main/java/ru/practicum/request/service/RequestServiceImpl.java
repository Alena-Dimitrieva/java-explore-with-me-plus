package ru.practicum.request.service;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.client.EventClient;
import ru.practicum.event.dto.EventInternalDto;
import ru.practicum.request.dao.RequestRepository;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.ParticipationStatus;
import ru.practicum.request.dto.RequestUpdateStatus;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.util.error.exception.ConflictException;
import ru.practicum.request.util.error.exception.NotFoundException;
import ru.practicum.user.client.UserClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(
		level = AccessLevel.PRIVATE,
		makeFinal = true
)
public class RequestServiceImpl
		implements RequestService {

	RequestRepository requestRepository;
	UserClient userClient;
	EventClient eventClient;

	@Override
	public List<ParticipationRequestDto> findByEventId(
			Long userId,
			Long eventId) {

		EventInternalDto event =
				getEventById(eventId);

		if (!event.initiatorId().equals(userId)) {
			throw new NotFoundException(
					"Событие не найдено"
			);
		}

		return requestRepository
				.findByEventId(eventId)
				.stream()
				.map(
						RequestMapper
								::toParticipationRequestDto
				)
				.toList();
	}

	@Override
	@Transactional
	public EventRequestStatusUpdateResult
	updateStatusRequest(
			Long userId,
			Long eventId,
			EventRequestStatusUpdateRequest request) {

		EventInternalDto event =
				getEventById(eventId);

		if (!event.initiatorId().equals(userId)) {
			throw new NotFoundException(
					"Событие не найдено"
			);
		}

		int limit =
				event.participantLimit();

		List<ParticipationRequestDto>
				confirmedRequests =
				new ArrayList<>();

		List<ParticipationRequestDto>
				rejectedRequests =
				new ArrayList<>();

		boolean moderationOff =
				!event.requestModeration()
						|| limit == 0;

		boolean idsEmpty =
				request.requestIds() == null
						|| request.requestIds().isEmpty();

		if (moderationOff || idsEmpty) {

			return EventRequestStatusUpdateResult
					.builder()
					.confirmedRequests(
							Collections.emptyList()
					)
					.rejectedRequests(
							Collections.emptyList()
					)
					.build();
		}

		int countConfirmed =
				requestRepository
						.countByEventIdAndStatus(
								eventId,
								ParticipationStatus.CONFIRMED
						);

		List<ParticipationRequest> requests =
				requestRepository.findAllByIdIn(
						request.requestIds()
				);

		boolean confirming =
				request.status()
						== RequestUpdateStatus.CONFIRMED;

		if (confirming
				&& countConfirmed >= limit) {

			throw new ConflictException(
					"Достигнут лимит "
							+ "подтвержденных заявок"
			);
		}

		for (ParticipationRequest pr :
				requests) {

			if (pr.getStatus()
					!= ParticipationStatus.PENDING) {

				throw new ConflictException(
						"Статус можно изменить "
								+ "только у заявок "
								+ "в состоянии рассмотрения"
				);
			}

			if (confirming
					&& countConfirmed < limit) {

				pr.setStatus(
						ParticipationStatus.CONFIRMED
				);

				countConfirmed++;

				confirmedRequests.add(
						RequestMapper
								.toParticipationRequestDto(
										pr
								)
				);

			} else {

				pr.setStatus(
						ParticipationStatus.REJECTED
				);

				rejectedRequests.add(
						RequestMapper
								.toParticipationRequestDto(
										pr
								)
				);
			}
		}

		requestRepository.saveAll(requests);

		if (confirming
				&& countConfirmed >= limit) {

			requestRepository.rejectPendingRequests(
					eventId,
					ParticipationStatus.PENDING
			);
		}

		return EventRequestStatusUpdateResult
				.builder()
				.confirmedRequests(
						confirmedRequests
				)
				.rejectedRequests(
						rejectedRequests
				)
				.build();
	}

	@Override
	public List<ParticipationRequestDto>
	findByRequesterId(Long userId) {

		return requestRepository
				.findByRequesterId(userId)
				.stream()
				.map(
						RequestMapper
								::toParticipationRequestDto
				)
				.toList();
	}

	@Override
	@Transactional
	public ParticipationRequestDto
	addParticipationRequest(
			Long userId,
			Long eventId) {

		if (!userClient.exists(userId)) {

			throw new NotFoundException(
					"Пользователь с id="
							+ userId
							+ " не найден"
			);
		}

		EventInternalDto event =
				getEventById(eventId);

		if (!event.published()) {

			throw new ConflictException(
					"Нельзя участвовать "
							+ "в неопубликованном событии"
			);
		}

		if (requestRepository
				.existsByRequesterIdAndEventId(
						userId,
						eventId
				)) {

			throw new ConflictException(
					"Запрос уже существует"
			);
		}

		if (event.initiatorId()
				.equals(userId)) {

			throw new ConflictException(
					"Инициатор события "
							+ "не может добавить запрос "
							+ "на участие в своём событии"
			);
		}

		int limit =
				event.participantLimit();

		if (limit != 0) {

			long confirmedCount =
					requestRepository
							.countByEventIdAndStatus(
									eventId,
									ParticipationStatus.CONFIRMED
							);

			if (event.requestModeration()) {

				long pendingCount =
						requestRepository
								.countByEventIdAndStatus(
										eventId,
										ParticipationStatus.PENDING
								);

				if (confirmedCount
						+ pendingCount >= limit) {

					throw new ConflictException(
							"Достигнут лимит "
									+ "запросов на участие"
					);
				}

			} else if (
					confirmedCount >= limit) {

				throw new ConflictException(
						"Достигнут лимит "
								+ "запросов на участие"
				);
			}
		}

		ParticipationStatus status =
				!event.requestModeration()
						|| limit == 0
						? ParticipationStatus.CONFIRMED
						: ParticipationStatus.PENDING;

		ParticipationRequest participationRequest =
				ParticipationRequest.builder()
						.requesterId(userId)
						.eventId(eventId)
						.status(status)
						.created(
								LocalDateTime.now()
										.truncatedTo(
												ChronoUnit.MILLIS
										)
						)
						.build();

		return RequestMapper
				.toParticipationRequestDto(
						requestRepository.save(
								participationRequest
						)
				);
	}

	@Override
	@Transactional
	public ParticipationRequestDto
	cancelParticipationRequest(
			Long userId,
			Long requestId) {

		ParticipationRequest request =
				getRequestById(requestId);

		if (!request.getRequesterId()
				.equals(userId)) {

			throw new ConflictException(
					"Нельзя отменить чужую заявку"
			);
		}

		request.setStatus(
				ParticipationStatus.CANCELED
		);

		return RequestMapper
				.toParticipationRequestDto(
						requestRepository.save(request)
				);
	}

	private EventInternalDto getEventById(
			long eventId) {

		try {

			return eventClient.getEvent(
					eventId
			);

		} catch (
				FeignException.NotFound ex) {

			throw new NotFoundException(
					"Событие с id="
							+ eventId
							+ " не найдено"
			);
		}
	}

	@NonNull
	private ParticipationRequest getRequestById(
			Long requestId) {

		return requestRepository
				.findById(requestId)
				.orElseThrow(
						() ->
								new NotFoundException(
										"Заявка с id="
												+ requestId
												+ " не найдена"
								)
				);
	}
}