package ru.practicum.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.user.dto.UserShortDto;

import java.util.List;

@FeignClient(
        name = "user-service",
        path = "/internal/users",
        fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    @GetMapping("/{userId}")
    UserShortDto getUserShort(@PathVariable("userId") Long userId);

    @GetMapping("/batch")
    List<UserShortDto> getUsersShort(@RequestParam("ids") List<Long> ids);

    @GetMapping("/{userId}/exists")
    boolean exists(@PathVariable("userId") Long userId);
}
