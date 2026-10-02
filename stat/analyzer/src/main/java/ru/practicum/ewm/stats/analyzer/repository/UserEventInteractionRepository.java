package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.UserEventId;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteraction;

import java.util.Collection;
import java.util.List;

public interface UserEventInteractionRepository
        extends JpaRepository<UserEventInteraction, UserEventId> {

    @Query("""
            select i
            from UserEventInteraction i
            where i.id.userId = :userId
            order by i.lastInteractionAt desc
            """)
    List<UserEventInteraction> findRecentByUserId(
            @Param("userId") long userId,
            Pageable pageable);

    List<UserEventInteraction> findByIdUserId(long userId);

    List<UserEventInteraction> findByIdUserIdAndIdEventIdIn(
            long userId,
            Collection<Long> eventIds);

    @Query("""
            select coalesce(sum(i.weight), 0)
            from UserEventInteraction i
            where i.id.eventId = :eventId
            """)
    long sumWeightsByEventId(@Param("eventId") long eventId);
}

