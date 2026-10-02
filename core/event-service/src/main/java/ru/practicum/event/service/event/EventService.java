package ru.practicum.event.service.event;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.event.*;

import java.util.List;

@Transactional
public interface EventService {

    List<EventShortDto> getFreeEvents(FreeGetDto freeGetDto);

    EventFullDto getFreeEventById(Long eventId, long userId);

    EventFullDto userAddNewEvent(Long userId, NewEventDto newEventDto);

    @Transactional(readOnly = true)
    List<EventFullDto> adminGetEvents(AdminGetDto adminGetDto);

    EventFullDto adminUpdateEvent(
            Long eventId,
            UpdateEventAdminRequest request
    );

    List<EventShortDto> findByUserId(
            Long userId,
            Integer from,
            Integer size
    );

    EventFullDto findEventById(
            Long userId,
            Long eventId
    );

    EventFullDto patchEvent(
            Long userId,
            Long eventId,
            UpdateEventUserRequest request
    );

    List<EventShortDto> getRecommendedEvents(long userId);

    void likeEvent(long userId, long eventId);
}