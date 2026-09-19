package ru.practicum.event.service.event;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.event.*;

import java.util.List;

@Transactional
public interface EventService {

    /// Получение событий незарегистрированным пользователем с возможностью фильтрации
    List<EventShortDto> getFreeEvents(FreeGetDto freeGetDto, HttpServletRequest request);

    /**
     * Получение незарегистрированным пользователем подробной информации об опубликованном событии
     * по его идентификатору
     */
    EventFullDto getFreeEventById(Long eventId, HttpServletRequest request);

    /// Добавление нового события зарегистрированным пользователем
    EventFullDto userAddNewEvent(Long userId, NewEventDto newEventDto);

    /// Поиск событий администратором
    @Transactional(readOnly = true)
    List<EventFullDto> adminGetEvents(AdminGetDto adminGetDto);

    /// Редактирование администратором данных события и его статуса (отклонение/публикация)
    EventFullDto adminUpdateEvent(Long eventId, UpdateEventAdminRequest request);

    /// Получение событий, добавленных текущим пользователем
    List<EventShortDto> findByUserId(Long userId, Integer from, Integer size);

    /// Получение полной информации о событии добавленном текущим пользователем
    EventFullDto findEventById(Long userId, Long eventId);

    /// Изменение события добавленного текущим пользователем
    EventFullDto patchEvent(Long userId, Long eventId, UpdateEventUserRequest request);
}
