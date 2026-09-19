package ru.practicum.user.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.dao.UserRepository;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.util.error.exception.NotFoundException;

import java.util.List;

/**
 * Служебные эндпоинты для межсервисного взаимодействия (Feign).
 * Не проксируются через Gateway — сервисы обращаются сюда напрямую через Eureka.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public UserShortDto getUserShort(
            @PathVariable Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new NotFoundException(
                                "Пользователь с id="
                                        + userId
                                        + " не найден"
                        )
                );

        return UserMapper.toUserShortDto(user);
    }

    @GetMapping("/{userId}/exists")
    public boolean exists(
            @PathVariable Long userId) {

        return userRepository.existsById(userId);
    }

    @GetMapping("/batch")
    public List<UserShortDto> getUsersShort(
            @RequestParam("ids") List<Long> ids) {

        return userRepository.findAllById(ids)
                .stream()
                .map(UserMapper::toUserShortDto)
                .toList();
    }
}
