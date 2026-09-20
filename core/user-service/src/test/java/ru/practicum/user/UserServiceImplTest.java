package ru.practicum.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.user.dao.UserRepository;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserServiceImpl;
import ru.practicum.user.util.error.exception.ConflictException;
import ru.practicum.user.util.error.exception.NotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void addNewUser_shouldSaveUser() {
        NewUserRequest request = NewUserRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();

        User saved = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();
        saved.setId(1L);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserDto result = service.adminAddNewUser(request);

        assertEquals(1L, result.id());
        assertEquals("Alice", result.name());
        assertEquals("alice@example.com", result.email());
    }

    @Test
    void addNewUser_shouldThrowConflictForDuplicateEmail() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        NewUserRequest request = NewUserRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();

        assertThrows(
                ConflictException.class,
                () -> service.adminAddNewUser(request)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUsers_shouldUseRequestedIds() {
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();
        user.setId(1L);

        when(userRepository.findAllByIdIn(
                List.of(1L),
                PageRequest.of(0, 10)
        )).thenReturn(List.of(user));

        List<UserDto> result = service.getUsers(List.of(1L), 0, 10);

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
        verify(userRepository).findAllByIdIn(List.of(1L), PageRequest.of(0, 10));
    }

    @Test
    void getUsers_shouldReturnPageWhenIdsAreEmpty() {
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();
        user.setId(1L);

        when(userRepository.findAll(PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(List.of(user)));

        List<UserDto> result = service.getUsers(List.of(), 10, 10);

        assertEquals(1, result.size());
        verify(userRepository).findAll(PageRequest.of(1, 10));
    }

    @Test
    void deleteUser_shouldDeleteExistingUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        service.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> service.deleteUser(999L)
        );

        verify(userRepository, never()).deleteById(anyLong());
    }
}
