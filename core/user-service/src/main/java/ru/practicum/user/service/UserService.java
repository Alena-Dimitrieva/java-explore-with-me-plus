package ru.practicum.user.service;

import jakarta.transaction.Transactional;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {

    @Transactional
    UserDto adminAddNewUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    @Transactional
    void deleteUser(Long userId);
}
