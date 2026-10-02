package ru.practicum.ewm.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SimilarityCalculator {

    private static final String TOPIC = "stats.events-similarity.v1";

    private final KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;

    private final Map<Long, Map<Long, Integer>> userActionsByEvent = new HashMap<>();
    private final Map<Long, Integer> eventWeights = new HashMap<>();
    private final Map<EventPair, Double> minWeightsByPair = new HashMap<>();

    public synchronized void process(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        int newWeight = getWeight(action.getActionType());

        Map<Long, Integer> users = userActionsByEvent.computeIfAbsent(
                eventId,
                ignored -> new HashMap<>()
        );

        Integer oldWeight = users.get(userId);

        if (oldWeight != null && oldWeight >= newWeight) {
            return;
        }

        boolean newEvent = !eventWeights.containsKey(eventId);

        users.put(userId, newWeight);
        eventWeights.merge(
                eventId,
                newWeight - (oldWeight == null ? 0 : oldWeight),
                Integer::sum
        );

        long timestamp = action.getTimestamp().toEpochMilli();

        if (newEvent) {
            calculateSimilarityForNewEvent(eventId, timestamp);
        } else {
            updateSimilarityForExistingEvent(
                    eventId,
                    userId,
                    oldWeight == null ? 0 : oldWeight,
                    newWeight,
                    timestamp
            );
        }
    }

    private void calculateSimilarityForNewEvent(long eventId, long timestamp) {
        Set<Long> existingEvents = new HashSet<>(eventWeights.keySet());
        existingEvents.remove(eventId);

        for (Long otherEventId : existingEvents) {
            updatePair(eventId, otherEventId, timestamp);
        }
    }

    private void updateSimilarityForExistingEvent(
            long eventId,
            long userId,
            int oldWeight,
            int newWeight,
            long timestamp) {
        Set<Long> otherEvents = new HashSet<>(eventWeights.keySet());
        otherEvents.remove(eventId);

        for (Long otherEventId : otherEvents) {
            EventPair pair = EventPair.of(eventId, otherEventId);

            Map<Long, Integer> otherUsers = userActionsByEvent.getOrDefault(
                    otherEventId,
                    Map.of()
            );

            int otherWeight = otherUsers.getOrDefault(userId, 0);
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);

            if (oldMin != newMin) {
                minWeightsByPair.merge(
                        pair,
                        newMin - oldMin,
                        Double::sum
                );
            }

            publishSimilarity(pair, timestamp);
        }
    }

    private void updatePair(long eventA, long eventB, long timestamp) {
        EventPair pair = EventPair.of(eventA, eventB);

        Map<Long, Integer> usersA = userActionsByEvent.getOrDefault(eventA, Map.of());
        Map<Long, Integer> usersB = userActionsByEvent.getOrDefault(eventB, Map.of());

        Set<Long> users = new HashSet<>(usersA.keySet());
        users.retainAll(usersB.keySet());

        double minWeight = 0.0;
        for (Long userId : users) {
            minWeight += Math.min(
                    usersA.get(userId),
                    usersB.get(userId)
            );
        }

        minWeightsByPair.put(pair, minWeight);
        publishSimilarity(pair, timestamp);
    }

    private void publishSimilarity(EventPair pair, long timestamp) {
        double minWeight = minWeightsByPair.getOrDefault(pair, 0.0);
        int weightA = eventWeights.getOrDefault(pair.eventA(), 0);
        int weightB = eventWeights.getOrDefault(pair.eventB(), 0);

        if (weightA == 0 || weightB == 0) {
            return;
        }

        double score = minWeight / Math.sqrt((double) weightA * weightB);

        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(pair.eventA())
                .setEventB(pair.eventB())
                .setScore(score)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .build();

        kafkaTemplate.send(TOPIC, pair.eventA(), similarity);
    }

    private int getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 1;
            case REGISTER -> 3;
            case LIKE -> 5;
        };
    }

    private record EventPair(long eventA, long eventB) {
        static EventPair of(long first, long second) {
            return first < second
                    ? new EventPair(first, second)
                    : new EventPair(second, first);
        }
    }
}