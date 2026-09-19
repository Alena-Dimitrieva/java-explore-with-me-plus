package ru.practicum.rating.service;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private RatingServiceImpl service;

    @Test
    void addReaction_shouldCreateRating() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 0, false
        );

        when(userClient.exists(200L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);
        when(ratingRepository.findByUserIdAndEventId(200L, 10L))
                .thenReturn(Optional.empty());

        when(ratingRepository.save(any(Rating.class)))
                .thenAnswer(invocation -> {
                    Rating rating = invocation.getArgument(0);
                    rating.setId(1L);
                    return rating;
                });

        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.LIKE))
                .thenReturn(1L);
        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.DISLIKE))
                .thenReturn(0L);

        RatingResponse result = service.addOrUpdateReaction(
                200L,
                10L,
                RatingRequest.builder()
                        .reaction(Reaction.LIKE)
                        .build()
        );

        assertEquals(1L, result.getId());
        assertEquals(200L, result.getUserId());
        assertEquals(10L, result.getEventId());
        assertEquals(Reaction.LIKE, result.getReaction());

        verify(ratingRepository).save(any(Rating.class));
        verify(eventClient).updateRate(10L, 1L);
    }

    @Test
    void addReaction_shouldUpdateExistingRating() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 0, false
        );

        Rating rating = Rating.builder()
                .userId(200L)
                .eventId(10L)
                .reaction(Reaction.LIKE)
                .build();
        rating.setId(1L);

        when(userClient.exists(200L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);
        when(ratingRepository.findByUserIdAndEventId(200L, 10L))
                .thenReturn(Optional.of(rating));
        when(ratingRepository.save(rating)).thenReturn(rating);
        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.LIKE))
                .thenReturn(0L);
        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.DISLIKE))
                .thenReturn(1L);

        RatingResponse result = service.addOrUpdateReaction(
                200L,
                10L,
                RatingRequest.builder().reaction(Reaction.DISLIKE).build()
        );

        assertEquals(Reaction.DISLIKE, result.getReaction());
        verify(ratingRepository).save(rating);
        verify(eventClient).updateRate(10L, -1L);
    }

    @Test
    void addReaction_shouldThrowWhenUserDoesNotExist() {
        when(userClient.exists(200L)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> service.addOrUpdateReaction(
                        200L,
                        10L,
                        RatingRequest.builder().reaction(Reaction.LIKE).build()
                )
        );

        verifyNoInteractions(eventClient, ratingRepository);
    }

    @Test
    void addReaction_shouldRejectInitiator() {
        EventInternalDto event = new EventInternalDto(
                10L, 200L, true, 0, false
        );

        when(userClient.exists(200L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);

        assertThrows(
                ValidationException.class,
                () -> service.addOrUpdateReaction(
                        200L,
                        10L,
                        RatingRequest.builder().reaction(Reaction.LIKE).build()
                )
        );

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void addReaction_shouldRemoveSameReaction() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 0, false
        );

        Rating rating = Rating.builder()
                .userId(200L)
                .eventId(10L)
                .reaction(Reaction.LIKE)
                .build();
        rating.setId(1L);

        when(userClient.exists(200L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);
        when(ratingRepository.findByUserIdAndEventId(200L, 10L))
                .thenReturn(Optional.of(rating));
        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.LIKE))
                .thenReturn(0L);
        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.DISLIKE))
                .thenReturn(0L);

        assertThrows(
                ConflictException.class,
                () -> service.addOrUpdateReaction(
                        200L,
                        10L,
                        RatingRequest.builder().reaction(Reaction.LIKE).build()
                )
        );

        verify(ratingRepository).delete(rating);
        verify(eventClient).updateRate(10L, 0L);
    }

    @Test
    void removeReaction_shouldDeleteExistingRating() {
        Rating rating = Rating.builder()
                .userId(200L)
                .eventId(10L)
                .reaction(Reaction.LIKE)
                .build();
        rating.setId(1L);

        when(ratingRepository.findByUserIdAndEventId(200L, 10L))
                .thenReturn(Optional.of(rating));
        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.LIKE))
                .thenReturn(0L);
        when(ratingRepository.countByEventIdAndReaction(10L, Reaction.DISLIKE))
                .thenReturn(0L);

        service.removeReaction(200L, 10L);

        verify(ratingRepository).delete(rating);
        verify(eventClient).updateRate(10L, 0L);
    }
}

