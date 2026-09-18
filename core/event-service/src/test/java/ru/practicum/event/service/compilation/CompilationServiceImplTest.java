package ru.practicum.event.service.compilation;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.event.dao.CompilationRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.compilation.CompilationDto;
import ru.practicum.event.dto.compilation.NewCompilationDto;
import ru.practicum.event.model.Compilation;
import ru.practicum.event.util.error.exception.NotFoundException;
import ru.practicum.event.util.statistic.StatRepository;
import ru.practicum.request.client.RequestClient;
import ru.practicum.user.client.UserClient;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private StatRepository statRepository;

    @Mock
    private RequestClient requestClient;

    @Mock
    private UserClient userClient;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private CompilationServiceImpl service;

    @Test
    void addCompilation_shouldCreateEmptyCompilation() {
        NewCompilationDto request = NewCompilationDto.builder()
                .title("Popular")
                .pinned(true)
                .events(Collections.emptySet())
                .build();

        Compilation saved = Compilation.builder()
                .title("Popular")
                .pinned(true)
                .build();
        saved.setId(10L);

        when(compilationRepository.save(any(Compilation.class))).thenReturn(saved);

        CompilationDto result = service.addCompilation(request);

        assertEquals(10L, result.getId());
        assertEquals("Popular", result.getTitle());
        assertTrue(result.isPinned());
        assertNotNull(result.getEvents());
        assertTrue(result.getEvents().isEmpty());
    }

    @Test
    void deleteCompilation_shouldDeleteExistingCompilation() {
        Compilation compilation = Compilation.builder()
                .title("Popular")
                .build();
        compilation.setId(10L);

        when(compilationRepository.findById(10L))
                .thenReturn(Optional.of(compilation));

        service.delById(10L);

        verify(compilationRepository).deleteById(10L);
    }

    @Test
    void deleteCompilation_shouldThrowWhenNotFound() {
        when(compilationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.delById(999L)
        );

        verify(compilationRepository, never()).deleteById(anyLong());
    }
}
