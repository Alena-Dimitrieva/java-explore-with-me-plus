package ru.practicum.ewm.stats.aggregator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimilarityCalculatorTest {

    private static final String TOPIC = "stats.events-similarity.v1";

    @Mock
    private KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;

    private SimilarityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SimilarityCalculator(kafkaTemplate);
    }

    private UserActionAvro action(long userId, long eventId, ActionTypeAvro type) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(Instant.ofEpochMilli(1_000L))
                .build();
    }

    @Test
    void firstActionOnFirstEvent_shouldNotPublishAnySimilarity() {
        calculator.process(action(1L, 100L, ActionTypeAvro.VIEW));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void whenSingleUserInteractedWithBothEvents_shouldPublishSimilarityWithScoreOne() {
        calculator.process(action(1L, 100L, ActionTypeAvro.VIEW));
        calculator.process(action(1L, 200L, ActionTypeAvro.VIEW));

        ArgumentCaptor<EventSimilarityAvro> captor = ArgumentCaptor.forClass(EventSimilarityAvro.class);
        verify(kafkaTemplate).send(eq(TOPIC), any(Long.class), captor.capture());

        EventSimilarityAvro published = captor.getValue();
        assertEquals(100L, published.getEventA());
        assertEquals(200L, published.getEventB());
        assertEquals(1.0, published.getScore(), 1e-9);
    }

    @Test
    void pairShouldBeOrdered_soEventAIsAlwaysSmaller() {
        // Сначала взаимодействие с большим id, потом с меньшим
        calculator.process(action(1L, 500L, ActionTypeAvro.VIEW));
        calculator.process(action(1L, 300L, ActionTypeAvro.VIEW));

        ArgumentCaptor<EventSimilarityAvro> captor = ArgumentCaptor.forClass(EventSimilarityAvro.class);
        verify(kafkaTemplate).send(eq(TOPIC), any(Long.class), captor.capture());

        EventSimilarityAvro published = captor.getValue();
        assertEquals(300L, published.getEventA());
        assertEquals(500L, published.getEventB());
    }

    @Test
    void repeatedActionWithLowerOrEqualWeight_shouldNotTriggerRecalculation() {
        calculator.process(action(1L, 100L, ActionTypeAvro.LIKE));  // вес 5
        calculator.process(action(1L, 200L, ActionTypeAvro.LIKE));  // вес 5, пара создана
        clearInvocations(kafkaTemplate);

        calculator.process(action(1L, 100L, ActionTypeAvro.VIEW));

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void repeatedActionWithHigherWeight_shouldRecalculateSimilarity() {
        calculator.process(action(1L, 100L, ActionTypeAvro.VIEW));
        calculator.process(action(1L, 200L, ActionTypeAvro.VIEW));
        clearInvocations(kafkaTemplate);
        calculator.process(action(1L, 100L, ActionTypeAvro.LIKE));

        verify(kafkaTemplate, atLeastOnce()).send(eq(TOPIC), any(Long.class), any(EventSimilarityAvro.class));
    }

    @Test
    void differentUsers_shouldNotContributeToSimilarityOfUnrelatedEvents() {
        // user1 -> event100, user2 -> event200: нет общего пользователя
        calculator.process(action(1L, 100L, ActionTypeAvro.VIEW));
        calculator.process(action(2L, 200L, ActionTypeAvro.VIEW));

        ArgumentCaptor<EventSimilarityAvro> captor = ArgumentCaptor.forClass(EventSimilarityAvro.class);
        verify(kafkaTemplate).send(eq(TOPIC), any(Long.class), captor.capture());
        // S_min = 0, similarity = 0
        assertEquals(0.0, captor.getValue().getScore(), 1e-9);
    }
}
