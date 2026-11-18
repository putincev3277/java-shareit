package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

/**
 * Сервис для управления пользователями.
 */
public interface UserService {

    /**
     * Создаёт нового пользователя.
     *
     * @param dto данные нового пользователя
     * @return созданный пользователь
     */
    UserDto create(UserCreateDto dto);

    /**
     * Возвращает пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return пользователь
     */
    UserDto getUserById(Long userId);

    /**
     * Частично обновляет пользователя: null-поля игнорируются.
     *
     * @param userId идентификатор пользователя
     * @param dto    поля для обновления
     * @return обновлённый пользователь
     */
    UserDto updateUser(Long userId, UserDto dto);

    /**
     * Ищет пользователей по фрагменту имени.
     *
     * @param nameFragment фрагмент имени для поиска
     * @return список найденных пользователей
     */
    List<UserDto> searchUsers(String nameFragment);

    /**
     * Удаляет пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     */
    void deleteUser(Long userId);
}
