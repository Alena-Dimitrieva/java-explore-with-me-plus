package ru.practicum.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.event.client.EventClient;
import ru.practicum.event.dto.EventInternalDto;
import ru.practicum.request.dao.RequestRepository;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.ParticipationStatus;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.util.error.exception.ConflictException;
import ru.practicum.request.util.error.exception.NotFoundException;
import ru.practicum.user.client.UserClient;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private RequestServiceImpl service;

    @Test
    void addParticipationRequest_shouldCreateConfirmedRequestWhenModerationDisabled() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 0, false
        );

        ParticipationRequest saved = ParticipationRequest.builder()
                .eventId(10L)
                .requesterId(200L)
                .status(ParticipationStatus.CONFIRMED)
                .created(LocalDateTime.now())
                .build();
        saved.setId(1L);

        when(userClient.exists(200L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);
        when(requestRepository.existsByRequesterIdAndEventId(200L, 10L))
                .thenReturn(false);
        when(requestRepository.save(any(ParticipationRequest.class)))
                .thenReturn(saved);

        ParticipationRequestDto result =
                service.addParticipationRequest(200L, 10L);

        assertEquals(1L, result.id());
        assertEquals(200L, result.requester());
        assertEquals(10L, result.event());
        assertEquals(ParticipationStatus.CONFIRMED, result.status());
    }

    @Test
    void addParticipationRequest_shouldCreatePendingRequestWhenModerationEnabled() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 2, true
        );

        ParticipationRequest saved = ParticipationRequest.builder()
                .eventId(10L)
                .requesterId(200L)
                .status(ParticipationStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
        saved.setId(1L);

        when(userClient.exists(200L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);
        when(requestRepository.existsByRequesterIdAndEventId(200L, 10L))
                .thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(
                10L, ParticipationStatus.CONFIRMED))
                .thenReturn(0);
        when(requestRepository.countByEventIdAndStatus(
                10L, ParticipationStatus.PENDING))
                .thenReturn(0);
        when(requestRepository.save(any(ParticipationRequest.class)))
                .thenReturn(saved);

        ParticipationRequestDto result =
                service.addParticipationRequest(200L, 10L);

        assertEquals(ParticipationStatus.PENDING, result.status());
    }

    @Test
    void addParticipationRequest_shouldRejectInitiator() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 2, true
        );

        when(userClient.exists(100L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);

        assertThrows(
                ConflictException.class,
                () -> service.addParticipationRequest(100L, 10L)
        );

        verify(requestRepository, never()).save(any());
    }

    @Test
    void addParticipationRequest_shouldRejectWhenLimitReached() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 1, false
        );

        when(userClient.exists(200L)).thenReturn(true);
        when(eventClient.getEvent(10L)).thenReturn(event);
        when(requestRepository.existsByRequesterIdAndEventId(200L, 10L))
                .thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(
                10L, ParticipationStatus.CONFIRMED))
                .thenReturn(1);

        assertThrows(
                ConflictException.class,
                () -> service.addParticipationRequest(200L, 10L)
        );

        verify(requestRepository, never()).save(any());
    }

    @Test
    void cancelParticipationRequest_shouldCancelOwnRequest() {
        ParticipationRequest request = ParticipationRequest.builder()
                .eventId(10L)
                .requesterId(200L)
                .status(ParticipationStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
        request.setId(1L);

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(request));
        when(requestRepository.save(request))
                .thenReturn(request);

        ParticipationRequestDto result =
                service.cancelParticipationRequest(200L, 1L);

        assertEquals(ParticipationStatus.CANCELED, result.status());
        verify(requestRepository).save(request);
    }

    @Test
    void cancelParticipationRequest_shouldRejectForeignRequest() {
        ParticipationRequest request = ParticipationRequest.builder()
                .eventId(10L)
                .requesterId(300L)
                .status(ParticipationStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
        request.setId(1L);

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        assertThrows(
                ConflictException.class,
                () -> service.cancelParticipationRequest(200L, 1L)
        );

        verify(requestRepository, never()).save(any());
    }

    @Test
    void findByEventId_shouldRejectNonInitiator() {
        EventInternalDto event = new EventInternalDto(
                10L, 100L, true, 2, true
        );

        when(eventClient.getEvent(10L)).thenReturn(event);

        assertThrows(
                NotFoundException.class,
                () -> service.findByEventId(200L, 10L)
        );

        verify(requestRepository, never()).findByEventId(anyLong());
    }
}

