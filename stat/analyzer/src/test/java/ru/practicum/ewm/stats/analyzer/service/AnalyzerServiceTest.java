package ru.practicum.ewm.stats.analyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.stats.analyzer.model.EventPairId;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.model.UserEventId;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteraction;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzerServiceTest {

    @Mock
    private UserEventInteractionRepository interactionRepository;

    @Mock
    private EventSimilarityRepository similarityRepository;

    @InjectMocks
    private AnalyzerService analyzerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(analyzerService, "neighbors", 5);
    }

    @Test
    void processUserAction_whenNewInteraction_shouldSaveWithActionWeight() {
        when(interactionRepository.findById(any(UserEventId.class)))
                .thenReturn(Optional.empty());

        UserActionAvro action = action(1L, 100L, ActionTypeAvro.LIKE);

        analyzerService.processUserAction(action);

        ArgumentCaptor<UserEventInteraction> captor = ArgumentCaptor.forClass(UserEventInteraction.class);
        verify(interactionRepository).save(captor.capture());

        UserEventInteraction saved = captor.getValue();
        assertEquals(1L, saved.getId().getUserId());
        assertEquals(100L, saved.getId().getEventId());
        assertEquals(5, saved.getWeight()); // LIKE = 5
        assertEquals(action.getTimestamp(), saved.getLastInteractionAt());
    }

    @Test
    void processUserAction_whenExistingWeightHigher_shouldKeepHigherWeight() {
        UserEventId id = new UserEventId(1L, 100L);
        UserEventInteraction existing = new UserEventInteraction(id, 5, Instant.ofEpochMilli(500L));
        when(interactionRepository.findById(id)).thenReturn(Optional.of(existing));

        analyzerService.processUserAction(action(1L, 100L, ActionTypeAvro.VIEW));

        ArgumentCaptor<UserEventInteraction> captor = ArgumentCaptor.forClass(UserEventInteraction.class);
        verify(interactionRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getWeight());
    }

    @Test
    void processUserAction_whenNewWeightHigher_shouldUpdateWeight() {
        UserEventId id = new UserEventId(1L, 100L);
        UserEventInteraction existing = new UserEventInteraction(id, 1, Instant.ofEpochMilli(500L));
        when(interactionRepository.findById(id)).thenReturn(Optional.of(existing));

        analyzerService.processUserAction(action(1L, 100L, ActionTypeAvro.LIKE));

        ArgumentCaptor<UserEventInteraction> captor = ArgumentCaptor.forClass(UserEventInteraction.class);
        verify(interactionRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getWeight());
    }

    @Test
    void processSimilarity_shouldSaveWithOrderedPair() {
        when(similarityRepository.findById(any(EventPairId.class)))
                .thenReturn(Optional.empty());

        EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                .setEventA(500L)
                .setEventB(300L)
                .setScore(0.75)
                .setTimestamp(Instant.ofEpochMilli(1_000L))
                .build();

        analyzerService.processSimilarity(avro);

        ArgumentCaptor<EventSimilarity> captor = ArgumentCaptor.forClass(EventSimilarity.class);
        verify(similarityRepository).save(captor.capture());

        EventSimilarity saved = captor.getValue();
        assertEquals(300L, saved.getId().getEventA());
        assertEquals(500L, saved.getId().getEventB());
        assertEquals(0.75, saved.getScore(), 1e-9);
    }

    @Test
    void processSimilarity_whenExisting_shouldUpdateScore() {
        EventPairId id = new EventPairId(300L, 500L);
        EventSimilarity existing = new EventSimilarity(id, 0.1, Instant.ofEpochMilli(100L));
        when(similarityRepository.findById(id)).thenReturn(Optional.of(existing));

        EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                .setEventA(300L)
                .setEventB(500L)
                .setScore(0.9)
                .setTimestamp(Instant.ofEpochMilli(2_000L))
                .build();

        analyzerService.processSimilarity(avro);

        ArgumentCaptor<EventSimilarity> captor = ArgumentCaptor.forClass(EventSimilarity.class);
        verify(similarityRepository).save(captor.capture());
        assertEquals(0.9, captor.getValue().getScore(), 1e-9);
    }

    @Test
    void hasUserInteracted_shouldReturnTrueWhenExists() {
        when(interactionRepository.existsById(new UserEventId(1L, 100L))).thenReturn(true);

        assertTrue(analyzerService.hasUserInteracted(1L, 100L));
    }

    @Test
    void hasUserInteracted_shouldReturnFalseWhenNotExists() {
        when(interactionRepository.existsById(new UserEventId(1L, 100L))).thenReturn(false);

        assertFalse(analyzerService.hasUserInteracted(1L, 100L));
    }

    @Test
    void getInteractionsCount_shouldReturnSumWeightsPerEvent() {
        when(interactionRepository.sumWeightsByEventId(100L)).thenReturn(7L);
        when(interactionRepository.sumWeightsByEventId(200L)).thenReturn(3L);

        List<AnalyzerService.ScoredEvent> result = analyzerService.getInteractionsCount(List.of(100L, 200L));

        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).eventId());
        assertEquals(7.0, result.get(0).score(), 1e-9);
        assertEquals(200L, result.get(1).eventId());
        assertEquals(3.0, result.get(1).score(), 1e-9);
    }

    @Test
    void getInteractionsCount_shouldIgnoreDuplicateEventIds() {
        when(interactionRepository.sumWeightsByEventId(100L)).thenReturn(7L);

        List<AnalyzerService.ScoredEvent> result = analyzerService.getInteractionsCount(List.of(100L, 100L, 100L));

        assertEquals(1, result.size());
        verify(interactionRepository, times(1)).sumWeightsByEventId(100L);
    }

    @Test
    void getSimilarEvents_whenMaxResultsZero_shouldReturnEmptyListWithoutDbCalls() {
        List<AnalyzerService.ScoredEvent> result = analyzerService.getSimilarEvents(100L, 1L, 0);

        assertTrue(result.isEmpty());
        verifyNoInteractions(interactionRepository);
        verifyNoInteractions(similarityRepository);
    }

    @Test
    void getSimilarEvents_shouldExcludeEventsUserAlreadyInteractedWith() {
        long eventId = 100L;
        long userId = 1L;

        UserEventInteraction interacted = new UserEventInteraction(
                new UserEventId(userId, 200L), 1, Instant.now());
        when(interactionRepository.findByIdUserId(userId)).thenReturn(List.of(interacted));

        EventSimilarity sim1 = new EventSimilarity(
                new EventPairId(100L, 200L), 0.9, Instant.now()); // пропустим, просмотрено
        EventSimilarity sim2 = new EventSimilarity(
                new EventPairId(100L, 300L), 0.5, Instant.now());
        when(similarityRepository.findByIdEventAOrIdEventB(eventId, eventId))
                .thenReturn(List.of(sim1, sim2));

        List<AnalyzerService.ScoredEvent> result = analyzerService.getSimilarEvents(eventId, userId, 10);

        assertEquals(1, result.size());
        assertEquals(300L, result.get(0).eventId());
        assertEquals(0.5, result.get(0).score(), 1e-9);
    }

    @Test
    void getSimilarEvents_shouldRespectMaxResultsAndSortDescending() {
        long eventId = 100L;
        long userId = 1L;
        when(interactionRepository.findByIdUserId(userId)).thenReturn(List.of());

        EventSimilarity s1 = new EventSimilarity(new EventPairId(100L, 201L), 0.3, Instant.now());
        EventSimilarity s2 = new EventSimilarity(new EventPairId(100L, 202L), 0.9, Instant.now());
        EventSimilarity s3 = new EventSimilarity(new EventPairId(100L, 203L), 0.6, Instant.now());
        when(similarityRepository.findByIdEventAOrIdEventB(eventId, eventId))
                .thenReturn(List.of(s1, s2, s3));

        List<AnalyzerService.ScoredEvent> result = analyzerService.getSimilarEvents(eventId, userId, 2);

        assertEquals(2, result.size());
        assertEquals(202L, result.get(0).eventId());
        assertEquals(203L, result.get(1).eventId());
    }

    @Test
    void getRecommendationsForUser_whenUserHasNoInteractions_shouldReturnEmpty() {
        when(interactionRepository.findByIdUserId(1L)).thenReturn(List.of());

        List<AnalyzerService.ScoredEvent> result = analyzerService.getRecommendationsForUser(1L, 10);

        assertTrue(result.isEmpty());
        verify(similarityRepository, never()).findByIdEventAOrIdEventB(anyLong(), anyLong());
    }

    @Test
    void getRecommendationsForUser_whenMaxResultsZero_shouldReturnEmpty() {
        List<AnalyzerService.ScoredEvent> result = analyzerService.getRecommendationsForUser(1L, 0);
        assertTrue(result.isEmpty());
    }

    private UserActionAvro action(long userId, long eventId, ActionTypeAvro type) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(Instant.ofEpochMilli(1_000L))
                .build();
    }
}

