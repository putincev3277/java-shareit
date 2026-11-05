package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.exception.EmailAlreadyExistsException;
import ru.practicum.shareit.exception.UserAlreadyExistsException;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserDto create(UserCreateDto dto) {
        User existing = userRepository.findByEmail(dto.getEmail());
        if (existing != null) {
            throw new EmailAlreadyExistsException("Email '" + dto.getEmail() + "' уже занят");
        }

        User newUser = new User();
        newUser.setName(dto.getName());
        newUser.setEmail(dto.getEmail());

        User saved = userRepository.save(newUser);
        return UserMapper.toUserDto(saved);
    }

    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
        }
        return UserMapper.toUserDto(user);
    }

    public UserDto updateUser(Long userId, UserDto dto) {
        User existing = userRepository.findById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
        }

        if (dto.getName() != null) {
            if (dto.getName().isBlank()) {
                throw new IllegalArgumentException("Имя не может быть пустым");
            }
            if (!dto.getName().equals(existing.getName())) {
                User other = userRepository.findByName(dto.getName());
                if (other != null) {
                    throw new UserAlreadyExistsException("Пользователь с именем '" + dto.getName() + "' уже существует");
                }
                existing.setName(dto.getName());
            }
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            User otherByEmail = userRepository.findByEmail(dto.getEmail());
            if (otherByEmail != null && !otherByEmail.getId().equals(userId)) {
                throw new EmailAlreadyExistsException("Email '" + dto.getEmail() + "' уже занят другим пользователем");
            }
            existing.setEmail(dto.getEmail());
        }

        User updated = userRepository.save(existing);
        return UserMapper.toUserDto(updated);
    }

    public List<UserDto> searchUsers(String nameFragment) {
        return userRepository.findByNameContaining(nameFragment).stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
        }
        userRepository.delete(userId);
    }
}
