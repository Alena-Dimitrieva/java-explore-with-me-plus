package ru.practicum.rating.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.client.EventClient;
import ru.practicum.event.dto.EventInternalDto;
import ru.practicum.rating.dao.RatingRepository;
import ru.practicum.rating.dto.RatingRequest;
import ru.practicum.rating.dto.RatingResponse;
import ru.practicum.rating.model.Rating;
import ru.practicum.rating.model.enums.Reaction;
import ru.practicum.rating.util.error.exception.ConflictException;
import ru.practicum.rating.util.error.exception.NotFoundException;
import ru.practicum.user.client.UserClient;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public RatingResponse addOrUpdateReaction(Long userId, Long eventId, RatingRequest request) {
        if (!userClient.exists(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        EventInternalDto event = getEvent(eventId);

        if (!event.published()) {
            throw new NotFoundException("Событие с id=" + eventId + " не существует или не опубликовано.");
        }

        if (Objects.equals(userId, event.initiatorId())) {
            throw new ValidationException("Нельзя ставить реакции своим событиям");
        }

        Rating rating = ratingRepository.findByUserIdAndEventId(userId, eventId).orElse(null);

        if (rating != null) {
            if (rating.getReaction() == request.getReaction()) {
                ratingRepository.delete(rating);
                updateEventRate(eventId);
                throw new ConflictException("Reaction removed");
            } else {
                rating.setReaction(request.getReaction());
                ratingRepository.save(rating);
                updateEventRate(eventId);
                return mapToResponse(rating);
            }
        } else {
            rating = Rating.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .reaction(request.getReaction())
                    .build();
            ratingRepository.save(rating);
            updateEventRate(eventId);
            return mapToResponse(rating);
        }
    }

    @Override
    public void removeReaction(Long userId, Long eventId) {
        Rating rating = ratingRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new NotFoundException("Реакция не найдена"));
        ratingRepository.delete(rating);
        updateEventRate(eventId);
    }

    private void updateEventRate(@NonNull Long eventId) {
        long likes = ratingRepository.countByEventIdAndReaction(eventId, Reaction.LIKE);
        long dislikes = ratingRepository.countByEventIdAndReaction(eventId, Reaction.DISLIKE);
        eventClient.updateRate(eventId, likes - dislikes);
    }

    @NonNull
    private EventInternalDto getEvent(Long eventId) {
        try {
            return eventClient.getEvent(eventId);
        } catch (Exception ex) {
            throw new NotFoundException("Событие с id=" + eventId + " не существует или не опубликовано.");
        }
    }

    private RatingResponse mapToResponse(@NonNull Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .userId(rating.getUserId())
                .eventId(rating.getEventId())
                .reaction(rating.getReaction())
                .build();
    }
}