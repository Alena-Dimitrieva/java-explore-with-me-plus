package ru.practicum.event.dao;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ru.practicum.event.model.Event;
import ru.practicum.event.model.enums.EventState;

import java.util.Collection;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>,
		JpaSpecificationExecutor<Event> {

	Collection<Event> findByInitiatorId(Long userId, PageRequest pageRequest);

	Optional<Event> findByIdAndState(Long eventId, EventState state);

	boolean existsByCategoryId(Long categoryId);

	boolean existsByIdAndState(Long eventId, EventState state);
}
