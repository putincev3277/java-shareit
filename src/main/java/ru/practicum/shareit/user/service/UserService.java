package ru.practicum.shareit.user.service;

import jakarta.validation.Valid;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;

/**
 * Сервис для управления пользователями.
 */
public interface UserService {

    /**
     * Возвращает пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     * @return пользователь
     */
    UserDto getUserById(Long id);

    /**
     * Обновляет данные пользователя.
     * Переданные null и пустые строки не затирают существующие значения.
     *
     * @param id  идентификатор пользователя
     * @param dto новые данные пользователя
     * @return обновлённый пользователь
     */
    UserDto updateUser(Long id, @Valid UserUpdateDto dto);

    /**
     * Удаляет пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     */
    void deleteUser(Long id);

    UserDto create(UserCreateDto dto);

    List<UserDto> searchUsers(String nameFragment);
}
