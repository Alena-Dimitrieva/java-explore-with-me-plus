package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.EventPairId;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.model.UserEventId;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteraction;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyzerService {

    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    @Value("${analyzer.recommendations.neighbors:5}")
    private int neighbors;

    @Transactional
    public synchronized void processUserAction(UserActionAvro action) {
        UserEventId id = new UserEventId(action.getUserId(), action.getEventId());
        UserEventInteraction interaction = interactionRepository.findById(id)
                .orElseGet(() -> new UserEventInteraction(id, 0, action.getTimestamp()));

        int newWeight = Math.max(interaction.getWeight(), weightOf(action.getActionType()));
        interaction.setWeight(newWeight);
        interaction.setLastInteractionAt(action.getTimestamp());
        interactionRepository.save(interaction);
    }

    @Transactional
    public synchronized void processSimilarity(EventSimilarityAvro message) {
        long first = Math.min(message.getEventA(), message.getEventB());
        long second = Math.max(message.getEventA(), message.getEventB());

        EventPairId id = new EventPairId(first, second);
        EventSimilarity similarity = similarityRepository.findById(id)
                .orElseGet(() -> new EventSimilarity(id, 0.0, message.getTimestamp()));

        similarity.setScore(message.getScore());
        similarity.setUpdatedAt(message.getTimestamp());
        similarityRepository.save(similarity);
    }

    @Transactional(readOnly = true)
    public List<ScoredEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        Set<Long> interacted = interactionRepository.findByIdUserId(userId)
                .stream()
                .map(item -> item.getId().getEventId())
                .collect(Collectors.toSet());

        Map<Long, Double> bestScores = new HashMap<>();

        for (EventSimilarity similarity : similarityRepository
                .findByIdEventAOrIdEventB(eventId, eventId)) {
            long candidate = similarity.getId().getEventA().equals(eventId)
                    ? similarity.getId().getEventB()
                    : similarity.getId().getEventA();

            if (candidate == eventId || interacted.contains(candidate)) {
                continue;
            }

            bestScores.merge(candidate, similarity.getScore(), Math::max);
        }

        return bestScores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> new ScoredEvent(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScoredEvent> getInteractionsCount(List<Long> eventIds) {
        return eventIds.stream()
                .distinct()
                .map(eventId -> new ScoredEvent(
                        eventId,
                        interactionRepository.sumWeightsByEventId(eventId)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScoredEvent> getRecommendationsForUser(long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserEventInteraction> allInteractions = interactionRepository.findByIdUserId(userId);
        if (allInteractions.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEvents = allInteractions.stream()
                .map(item -> item.getId().getEventId())
                .collect(Collectors.toSet());

        List<UserEventInteraction> recentInteractions = interactionRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, maxResults)
        );

        Set<Long> recentEvents = recentInteractions.stream()
                .map(item -> item.getId().getEventId())
                .collect(Collectors.toSet());

        Map<Long, Double> candidateSeedSimilarity = new HashMap<>();
        for (Long recentEvent : recentEvents) {
            for (EventSimilarity similarity : similarityRepository
                    .findByIdEventAOrIdEventB(recentEvent, recentEvent)) {
                long candidate = similarity.getId().getEventA().equals(recentEvent)
                        ? similarity.getId().getEventB()
                        : similarity.getId().getEventA();

                if (candidate == recentEvent || interactedEvents.contains(candidate)) {
                    continue;
                }

                candidateSeedSimilarity.merge(
                        candidate,
                        similarity.getScore(),
                        Math::max
                );
            }
        }

        if (candidateSeedSimilarity.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> userWeights = allInteractions.stream()
                .collect(Collectors.toMap(
                        item -> item.getId().getEventId(),
                        UserEventInteraction::getWeight,
                        Math::max
                ));

        List<ScoredEvent> result = new ArrayList<>();

        for (Long candidate : candidateSeedSimilarity.keySet()) {
            double predictedScore = predictScore(candidate, userWeights);
            if (predictedScore > 0.0) {
                result.add(new ScoredEvent(candidate, predictedScore));
            }
        }

        return result.stream()
                .sorted(Comparator.comparingDouble(ScoredEvent::score).reversed())
                .limit(maxResults)
                .toList();
    }

    private double predictScore(long candidate, Map<Long, Integer> userWeights) {
        List<Neighbor> neighborsList = new ArrayList<>();

        for (Map.Entry<Long, Integer> interaction : userWeights.entrySet()) {
            long interactedEvent = interaction.getKey();
            if (interactedEvent == candidate) {
                continue;
            }

            EventPairId pair = new EventPairId(
                    Math.min(candidate, interactedEvent),
                    Math.max(candidate, interactedEvent)
            );

            similarityRepository.findById(pair).ifPresent(similarity -> {
                if (similarity.getScore() > 0.0) {
                    neighborsList.add(new Neighbor(
                            interactedEvent,
                            interaction.getValue(),
                            similarity.getScore()
                    ));
                }
            });
        }

        neighborsList.sort(Comparator.comparingDouble(Neighbor::similarity).reversed());

        double weightedSum = 0.0;
        double similaritySum = 0.0;
        int limit = Math.min(neighbors, neighborsList.size());

        for (int i = 0; i < limit; i++) {
            Neighbor neighbor = neighborsList.get(i);
            weightedSum += neighbor.weight() * neighbor.similarity();
            similaritySum += neighbor.similarity();
        }

        if (similaritySum == 0.0) {
            return 0.0;
        }

        return weightedSum / similaritySum;
    }

    private int weightOf(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 1;
            case REGISTER -> 3;
            case LIKE -> 5;
        };
    }

    public record ScoredEvent(long eventId, double score) {
    }

    private record Neighbor(long eventId, int weight, double similarity) {
    }

    @Transactional(readOnly = true)
    public boolean hasUserInteracted(long userId, long eventId) {
        return interactionRepository.existsById(new UserEventId(userId, eventId));
    }
}

