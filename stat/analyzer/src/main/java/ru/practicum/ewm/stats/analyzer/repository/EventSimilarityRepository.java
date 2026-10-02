package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.stats.analyzer.model.EventPairId;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventPairId> {

    List<EventSimilarity> findByIdEventAOrIdEventB(long eventA, long eventB);

}
