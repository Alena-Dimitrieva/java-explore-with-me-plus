package ru.practicum.user.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.user.dto.UserShortDto;

import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.warn("user-service недоступен, используем fallback: {}", cause.getMessage());

        return new UserClient() {

            @Override
            public UserShortDto getUserShort(Long userId) {
                return UserShortDto.builder()
                        .id(userId)
                        .name("unknown")
                        .email("unknown@example.com")
                        .build();
            }

            @Override
            public List<UserShortDto> getUsersShort(List<Long> ids) {
                if (ids == null) {
                    return List.of();
                }
                return ids.stream()
                        .map(id -> UserShortDto.builder()
                                .id(id)
                                .name("unknown")
                                .email("unknown@example.com")
                                .build())
                        .toList();
            }

            @Override
            public boolean exists(Long userId) {
                // не блокирует операции, если user-service упал
                return true;
            }
        };
    }
}
