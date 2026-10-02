package ru.practicum.event.service.event;

import jakarta.validation.ValidationException;
import ru.practicum.event.util.error.exception.BadRequestException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.event.dao.CategoryRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.event.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.mapper.StateMapper;
import ru.practicum.event.model.Category;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.enums.AdminStateAction;
import ru.practicum.event.model.enums.EventState;
import ru.practicum.event.model.enums.UserStateAction;
import ru.practicum.event.service.request.RequestServiceClient;

import ru.practicum.event.util.error.exception.ConflictException;
import ru.practicum.event.util.error.exception.NotFoundException;
import ru.practicum.event.util.specification.EventSpecifications;
import ru.practicum.event.util.specification.SpecBuilder;
import ru.practicum.request.dto.EventRequestCountDto;
import ru.practicum.stat.client.AnalyzerClient;
import ru.practicum.stat.client.CollectorClient;
import ru.practicum.user.client.UserClient;
import ru.practicum.user.dto.UserShortDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventServiceImpl implements EventService {

    final AnalyzerClient analyzerClient;
    final CollectorClient collectorClient;
    final EventRepository eventRepository;
    final UserClient userClient;
    final CategoryRepository categoryRepository;
    final RequestServiceClient requestServiceClient;

    @Value("${event.recommendations.max-results:10}")
    int recommendationsMaxResults;

    @Override
    public List<EventShortDto> getFreeEvents(@NonNull FreeGetDto dto) {

        validateDateRange(dto.rangeStart(), dto.rangeEnd());

        SpecBuilder<Event> builder = SpecBuilder.<Event>builder()
                .and(EventSpecifications.isPublished())
                .andIf(dto.text() != null && !dto.text().isBlank(),
                        () -> EventSpecifications.textContains(dto.text()))
                .andIf(dto.categories() != null && !dto.categories().isEmpty(),
                        () -> EventSpecifications.hasCategories(dto.categories()))
                .andIf(dto.paid() != null,
                        () -> EventSpecifications.isPaid(dto.paid()));

        if (dto.rangeStart() == null && dto.rangeEnd() == null) {
            builder.and(EventSpecifications.eventDateAfterNow(LocalDateTime.now()));
        } else {
            builder.andIf(dto.rangeStart() != null,
                    () -> EventSpecifications.dateAfter(dto.rangeStart()));
            builder.andIf(dto.rangeEnd() != null,
                    () -> EventSpecifications.dateBefore(dto.rangeEnd()));
        }

        Specification<Event> specification = builder.build();

        boolean requiresInMemoryProcessing =
                Boolean.TRUE.equals(dto.onlyAvailable())
                        || dto.sort() == FreeGetDto.FreeEventSort.RATING;

        List<Event> events;

        if (requiresInMemoryProcessing) {
            events = eventRepository.findAll(specification);
        } else {
            Sort sort = dto.sort() == FreeGetDto.FreeEventSort.EVENT_DATE
                    ? Sort.by("eventDate").ascending()
                    : Sort.unsorted();

            Pageable pageable = PageRequest.of(
                    dto.from() / dto.size(),
                    dto.size(),
                    sort
            );

            events = eventRepository.findAll(specification, pageable).getContent();
        }

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> requestCountMap = getRequestCountMap(events);
        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratings = getRatings(events);

        if (Boolean.TRUE.equals(dto.onlyAvailable())) {
            events = events.stream()
                    .filter(event -> {
                        long confirmed = requestCountMap.getOrDefault(event.getId(), 0L);
                        return event.getParticipantLimit() == 0
                                || confirmed < event.getParticipantLimit();
                    })
                    .toList();
        }

        if (dto.sort() == FreeGetDto.FreeEventSort.RATING) {
            events = events.stream()
                    .sorted(Comparator.comparingDouble(
                            (Event event) -> ratings.getOrDefault(event.getId(), 0.0)
                    ).reversed())
                    .toList();
        } else if (dto.sort() == FreeGetDto.FreeEventSort.EVENT_DATE
                && requiresInMemoryProcessing) {
            events = events.stream()
                    .sorted(Comparator.comparing(Event::getEventDate))
                    .toList();
        }

        events = paginate(events, dto.from(), dto.size());

        return events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        initiatorsMap.get(event.getInitiatorId()),
                        requestCountMap.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 0.0)
                ))
                .toList();
    }

    @Override
    public EventFullDto getFreeEventById(Long eventId, long userId) {

        Event event = eventRepository
                .findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не существует или не опубликовано."
                ));

        try {
            collectorClient.collectUserAction(
                    userId,
                    eventId,
                    ActionTypeProto.ACTION_VIEW,
                    Instant.now()
            );
        } catch (Exception ex) {
            log.warn("Не удалось отправить ACTION_VIEW в Collector: {}", ex.getMessage());
        }

        long confirmedRequests = requestServiceClient.getConfirmedCount(eventId);
        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());

        return EventMapper.toEventFullDto(
                event,
                initiator,
                confirmedRequests,
                getRating(eventId)
        );
    }

    @Override
    @Transactional
    public EventFullDto userAddNewEvent(Long userId, @NonNull NewEventDto newEventDto) {

        validateParticipantLimit(newEventDto.participantLimit());
        validateEventDate(newEventDto.eventDate());
        checkUser(userId);

        Category category = getCategoryById(newEventDto.category());

        Event event = EventMapper.toEntity(
                newEventDto,
                category,
                LocalDateTime.now(),
                userId,
                null,
                EventState.PENDING
        );

        UserShortDto initiator = userClient.getUserShort(userId);
        Event saved = eventRepository.save(event);

        return EventMapper.toEventFullDto(saved, initiator, 0L, 0.0);
    }

    @Override
    public List<EventFullDto> adminGetEvents(@NonNull AdminGetDto dto) {

        Specification<Event> specification = SpecBuilder.<Event>builder()
                .andIf(dto.users() != null && !dto.users().isEmpty(),
                        () -> EventSpecifications.hasUsers(dto.users()))
                .andIf(dto.states() != null && !dto.states().isEmpty(),
                        () -> EventSpecifications.hasStates(dto.states()))
                .andIf(dto.categories() != null && !dto.categories().isEmpty(),
                        () -> EventSpecifications.hasCategories(dto.categories()))
                .andIf(dto.rangeStart() != null,
                        () -> EventSpecifications.dateAfter(dto.rangeStart()))
                .andIf(dto.rangeEnd() != null,
                        () -> EventSpecifications.dateBefore(dto.rangeEnd()))
                .build();

        Pageable pageable = PageRequest.of(dto.from() / dto.size(), dto.size());

        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> requestCountMap = getRequestCountMap(events);
        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratings = getRatings(events);

        return events.stream()
                .map(event -> EventMapper.toEventFullDto(
                        event,
                        initiatorsMap.get(event.getInitiatorId()),
                        requestCountMap.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 0.0)
                ))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto adminUpdateEvent(Long eventId,
                                         @NonNull UpdateEventAdminRequest request) {

        Event oldEvent = getEventById(eventId);

        validateParticipantLimit(request.participantLimit());
        if (request.eventDate() != null) {
            validateEventDate(request.eventDate());
        }

        EventState newState = oldEvent.getState();
        LocalDateTime publishedOn = oldEvent.getPublishedOn();

        if (request.stateAction() != null) {
            if (request.stateAction() == AdminStateAction.PUBLISH_EVENT) {
                if (oldEvent.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Событие с id=" + eventId + " уже опубликовано");
                }
                if (oldEvent.getState() == EventState.CANCELED) {
                    throw new ConflictException("Публикация отмененного события невозможна");
                }
                newState = StateMapper.mapAdminEventAction(request.stateAction());
                publishedOn = LocalDateTime.now();
            } else if (request.stateAction() == AdminStateAction.REJECT_EVENT) {
                if (oldEvent.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Опубликованное событие нельзя отклонить");
                }
                newState = StateMapper.mapAdminEventAction(request.stateAction());
                publishedOn = null;
            }
        }

        Category category = request.category() == null
                ? null
                : getCategoryById(request.category());

        Event updated = EventMapper.update(oldEvent, request, newState, publishedOn, category);
        Event saved = eventRepository.save(updated);

        UserShortDto initiator = userClient.getUserShort(saved.getInitiatorId());

        return EventMapper.toEventFullDto(
                saved,
                initiator,
                getConfirmedRequests(saved.getId()),
                getRating(saved.getId())
        );
    }

    @Override
    public List<EventShortDto> findByUserId(Long userId, Integer from, Integer size) {

        checkUser(userId);

        Pageable pageable = PageRequest.of(from / size, size);

        Collection<Event> events = eventRepository.findByInitiatorId(userId, (PageRequest) pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        UserShortDto initiator = userClient.getUserShort(userId);
        Map<Long, Long> requestCountMap = getRequestCountMap(events);
        Map<Long, Double> ratings = getRatings(events);

        return events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        initiator,
                        requestCountMap.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 0.0)
                ))
                .toList();
    }

    @Override
    public EventFullDto findEventById(Long userId, Long eventId) {

        checkUser(userId);
        Event event = getEventById(eventId);

        if (!userId.equals(event.getInitiatorId())) {
            throw new ConflictException("Пользователь должен быть инициатором");
        }

        UserShortDto initiator = userClient.getUserShort(userId);

        return EventMapper.toEventFullDto(
                event,
                initiator,
                getConfirmedRequests(event.getId()),
                getRating(event.getId())
        );
    }

    @Override
    @Transactional
    public EventFullDto patchEvent(Long userId, Long eventId,
                                   @NonNull UpdateEventUserRequest request) {

        validateParticipantLimit(request.participantLimit());
        checkUser(userId);

        Event event = getEventById(eventId);

        if (!userId.equals(event.getInitiatorId())) {
            throw new ConflictException("Пользователь должен быть инициатором");
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }
        if (request.eventDate() != null) {
            validateEventDate(request.eventDate());
        }

        UserStateAction action = request.stateAction();
        if (action != null) {
            if (action == UserStateAction.PUBLISH_EVENT || action == UserStateAction.REJECT_EVENT) {
                throw new ValidationException(
                        "Пользователь не может изменить событие на состояние " + action
                );
            }
            EventState newState = StateMapper.mapUserEventAction(action);
            if (newState != null) {
                event.setState(newState);
            }
        }

        if (request.category() != null) {
            Category category = getCategoryById(request.category());
            event.setCategory(category);
        }

        EventMapper.merge(event, request);

        try {
            Event patched = eventRepository.save(event);
            log.info("Ивент обновлен: {}", patched.getId());

            UserShortDto initiator = userClient.getUserShort(patched.getInitiatorId());

            return EventMapper.toEventFullDto(
                    patched,
                    initiator,
                    getConfirmedRequests(patched.getId()),
                    getRating(patched.getId())
            );
        } catch (DataIntegrityViolationException ex) {
            log.debug("Конфликт во время обновления ивента {}", event.getId(), ex);
            throw new ConflictException("Конфликт при обновлении события");
        }
    }

    @Override
    public List<EventShortDto> getRecommendedEvents(long userId) {

        List<Long> recommendedIds;
        try {
            recommendedIds = analyzerClient
                    .getRecommendationsForUser(userId, recommendationsMaxResults)
                    .map(RecommendedEventProto::getEventId)
                    .toList();
        } catch (Exception ex) {
            log.warn("Не удалось получить рекомендации для пользователя {}: {}", userId, ex.getMessage());
            return Collections.emptyList();
        }

        if (recommendedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Event> eventsById = eventRepository.findAllById(recommendedIds).stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .collect(java.util.stream.Collectors.toMap(Event::getId, e -> e));

        List<Event> events = recommendedIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .toList();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Long> requestCountMap = getRequestCountMap(events);
        Map<Long, Double> ratings = getRatings(events);

        return events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        initiatorsMap.get(event.getInitiatorId()),
                        requestCountMap.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 0.0)
                ))
                .toList();
    }

    @Override
    @Transactional
    public void likeEvent(long userId, long eventId) {

        getEventById(eventId);

        boolean interacted;
        try {
            interacted = analyzerClient.hasUserInteracted(userId, eventId);
        } catch (Exception ex) {
            log.warn("Не удалось проверить взаимодействие: {}", ex.getMessage());
            throw new BadRequestException(
                    "Не удалось проверить взаимодействие с мероприятием"
            );
        }

        if (!interacted) {
            throw new BadRequestException(
                    "Пользователь может лайкать только просмотренные мероприятия"
            );
        }

        try {
            collectorClient.collectUserAction(
                    userId,
                    eventId,
                    ActionTypeProto.ACTION_LIKE,
                    Instant.now()
            );
        } catch (Exception ex) {
            log.warn("Не удалось отправить ACTION_LIKE в Collector: {}", ex.getMessage());
        }
    }

    private double getRating(long eventId) {
        try {
            return analyzerClient.getInteractionsCount(List.of(eventId))
                    .filter(item -> item.getEventId() == eventId)
                    .map(RecommendedEventProto::getScore)
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception ex) {
            log.warn("Не удалось получить rating для события {}: {}", eventId, ex.getMessage());
            return 0.0;
        }
    }

    private Map<Long, Double> getRatings(Collection<Event> events) {

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .distinct()
                .toList();

        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> result = new HashMap<>();

        try {
            analyzerClient.getInteractionsCount(eventIds)
                    .forEach(item -> result.put(item.getEventId(), item.getScore()));
        } catch (Exception ex) {
            log.warn("Не удалось получить rating-и для событий: {}", ex.getMessage());
        }

        return result;
    }

    private void checkUser(Long userId) {
        if (!userClient.exists(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private Event getEventById(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено"
                ));
    }

    private Category getCategoryById(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Категория с id=" + categoryId + " не найдена"
                ));
    }

    private long getConfirmedRequests(Long eventId) {
        return requestServiceClient.getConfirmedCount(eventId);
    }

    private Map<Long, Long> getRequestCountMap(Collection<Event> events) {

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .distinct()
                .toList();

        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<EventRequestCountDto> counts = requestServiceClient.getConfirmedCounts(eventIds);

        Map<Long, Long> result = new HashMap<>();
        counts.forEach(count -> result.put(count.eventId(), count.count()));
        return result;
    }

    private Map<Long, UserShortDto> getInitiatorsMap(Collection<Event> events) {

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (initiatorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserShortDto> users = userClient.getUsersShort(initiatorIds);

        Map<Long, UserShortDto> result = new HashMap<>();
        users.forEach(user -> result.put(user.id(), user));
        return result;
    }

    private List<Event> paginate(List<Event> events, int from, int size) {
        if (from >= events.size()) {
            return List.of();
        }
        return events.subList(from, Math.min(from + size, events.size()));
    }

    private void validateDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new ValidationException("Окончание события не может быть раньше начала");
        }
    }

    private void validateParticipantLimit(Integer participantLimit) {
        if (participantLimit != null && participantLimit < 0) {
            throw new ValidationException(
                    "Ограничение на количество участников не может быть отрицательным"
            );
        }
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException(
                    "Дата события должна быть не ранее чем через 2 часа(ов)"
            );
        }
    }
}