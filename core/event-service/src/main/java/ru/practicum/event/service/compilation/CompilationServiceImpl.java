package ru.practicum.event.service.compilation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dao.CompilationRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.compilation.CompilationDto;
import ru.practicum.event.dto.compilation.CompilationSearchFilter;
import ru.practicum.event.dto.compilation.CompilationUpdateDto;
import ru.practicum.event.dto.compilation.NewCompilationDto;
import ru.practicum.event.mapper.CompilationMapper;
import ru.practicum.event.model.Compilation;
import ru.practicum.event.model.Event;
import ru.practicum.event.util.error.exception.NotFoundException;
import ru.practicum.event.util.statistic.StatRepository;
import ru.practicum.request.client.RequestClient;
import ru.practicum.request.dto.EventRequestCountDto;
import ru.practicum.stat.dto.ViewStatsDto;
import ru.practicum.user.client.UserClient;
import ru.practicum.user.dto.UserShortDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(
		level = AccessLevel.PRIVATE,
		makeFinal = true
)
public class CompilationServiceImpl
		implements CompilationService {

	CompilationRepository compilationRepository;
	EventRepository eventRepository;
	StatRepository statRepository;
	RequestClient requestClient;
	UserClient userClient;

	@Override
	public CompilationDto getById(
			Long compilationId,
			HttpServletRequest request) {

		statRepository.sendHitRequest(request);

		Compilation compilation =
				getCompilationById(compilationId);

		return CompilationMapper.toCompilationDto(
				compilation,
				getInitiators(
						List.of(compilation)
				),
				getConfirmedRequests(
						List.of(compilation)
				),
				getViews(
						List.of(compilation)
				)
		);
	}

	@Override
	@Transactional
	public void delById(Long compilationId) {

		getCompilationById(compilationId);

		compilationRepository.deleteById(
				compilationId
		);
	}

	@Override
	@Transactional
	public CompilationDto addCompilation(
			@NonNull NewCompilationDto compilationDto) {

		List<Long> requestedIds =
				compilationDto.getEvents() == null
						? List.of()
						: compilationDto.getEvents()
						.stream()
						.distinct()
						.toList();

		Set<Event> events =
				requestedIds.isEmpty()
						? new HashSet<>()
						: new HashSet<>(
						eventRepository.findAllById(
								requestedIds
						)
				);

		if (events.size() < requestedIds.size()) {
			throw new NotFoundException(
					"Одно или несколько событий не найдены"
			);
		}

		Compilation compilation =
				CompilationMapper.toEntity(
						compilationDto,
						events
				);

		Compilation savedCompilation =
				compilationRepository.save(compilation);

		Map<Long, Long> confirmedRequests =
				new HashMap<>();

		Map<Long, Long> views =
				new HashMap<>();

		savedCompilation.getEvents()
				.forEach(event -> {
					confirmedRequests.put(
							event.getId(),
							0L
					);

					views.put(
							event.getId(),
							0L
					);
				});

		return CompilationMapper.toCompilationDto(
				savedCompilation,
				getInitiators(
						List.of(savedCompilation)
				),
				confirmedRequests,
				views
		);
	}

	@Override
	@Transactional
	public CompilationDto updateCompilation(
			Long compilationId,
			@NonNull CompilationUpdateDto dto) {

		Compilation compilation =
				getCompilationById(compilationId);

		if (dto.getEvents() != null) {

			List<Long> requestedIds =
					dto.getEvents()
							.stream()
							.distinct()
							.toList();

			Set<Event> events =
					requestedIds.isEmpty()
							? new HashSet<>()
							: new HashSet<>(
							eventRepository.findAllById(
									requestedIds
							)
					);

			if (events.size() < requestedIds.size()) {
				throw new NotFoundException(
						"Одно или несколько событий не найдены"
				);
			}

			compilation.setEvents(events);
		}

		if (dto.getTitle() != null
				&& !dto.getTitle().isBlank()) {

			compilation.setTitle(
					dto.getTitle()
			);
		}

		if (dto.getPinned() != null) {
			compilation.setPinned(
					dto.getPinned()
			);
		}

		return CompilationMapper.toCompilationDto(
				compilation,
				getInitiators(
						List.of(compilation)
				),
				getConfirmedRequests(
						List.of(compilation)
				),
				getViews(
						List.of(compilation)
				)
		);
	}

	@Override
	public List<CompilationDto> getByFilter(
			@NonNull CompilationSearchFilter filter,
			HttpServletRequest request) {

		Pageable pageable =
				PageRequest.of(
						filter.getFrom() / filter.getSize(),
						filter.getSize()
				);

		Page<Compilation> page;

		if (filter.getPinned() != null) {
			page =
					compilationRepository.findAllByPinned(
							filter.getPinned(),
							pageable
					);
		} else {
			page =
					compilationRepository.findAll(
							pageable
					);
		}

		List<Compilation> compilations =
				page.getContent();

		if (compilations.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Long, UserShortDto> initiators =
				getInitiators(compilations);

		Map<Long, Long> confirmedRequests =
				getConfirmedRequests(compilations);

		Map<Long, Long> views =
				getViews(compilations);

		return compilations.stream()
				.map(compilation ->
						CompilationMapper.toCompilationDto(
								compilation,
								initiators,
								confirmedRequests,
								views
						)
				)
				.toList();
	}

	private Compilation getCompilationById(
			long compilationId) {

		return compilationRepository
				.findById(compilationId)
				.orElseThrow(
						() -> new NotFoundException(
								"Подборка с id="
										+ compilationId
										+ " не найдена"
						)
				);
	}

	private Map<Long, UserShortDto> getInitiators(
			Collection<Compilation> compilations) {

		List<Long> initiatorIds =
				compilations.stream()
						.flatMap(
								compilation ->
										compilation
												.getEvents()
												.stream()
						)
						.map(Event::getInitiatorId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();

		if (initiatorIds.isEmpty()) {
			return Collections.emptyMap();
		}

		return userClient
				.getUsersShort(initiatorIds)
				.stream()
				.collect(
						Collectors.toMap(
								UserShortDto::id,
								user -> user
						)
				);
	}

	private Map<Long, Long> getConfirmedRequests(
			Collection<Compilation> compilations) {

		List<Long> eventIds =
				compilations.stream()
						.flatMap(
								compilation ->
										compilation
												.getEvents()
												.stream()
						)
						.map(Event::getId)
						.distinct()
						.toList();

		if (eventIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<EventRequestCountDto> counts =
				requestClient.getConfirmedCounts(
						eventIds
				);

		return counts.stream()
				.collect(
						Collectors.toMap(
								EventRequestCountDto::eventId,
								EventRequestCountDto::count,
								(first, second) -> first
						)
				);
	}

	private Map<Long, Long> getViews(
			Collection<Compilation> compilations) {

		List<Long> eventIds =
				compilations.stream()
						.flatMap(
								compilation ->
										compilation
												.getEvents()
												.stream()
						)
						.map(Event::getId)
						.distinct()
						.toList();

		if (eventIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<String> uris =
				eventIds.stream()
						.map(id -> "/events/" + id)
						.toList();

		List<ViewStatsDto> stats =
				statRepository.getStat(
						uris,
						false
				);

		return stats.stream()
				.collect(
						Collectors.toMap(
								stat ->
										Long.parseLong(
												stat.getUri()
														.replace(
																"/events/",
																""
														)
										),
								ViewStatsDto::getHits,
								(first, second) -> first
						)
				);
	}
}