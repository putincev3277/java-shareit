package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserCreateDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        User user = UserMapper.toEntity(dto);
        User saved = userRepository.save(user);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));
        return UserMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserUpdateDto dto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            existing.setName(dto.getName());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String newEmail = dto.getEmail();
            if (existing.getEmail() == null || !existing.getEmail().equalsIgnoreCase(newEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new ConflictException("Пользователь с таким email уже существует");
                }
                existing.setEmail(newEmail);
            }
        }

        User updated = userRepository.save(existing);
        return UserMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String nameFragment) {
        if (nameFragment == null || nameFragment.isBlank()) {
            return List.of();
        }
        return userRepository.findByNameContainingIgnoreCase(nameFragment).stream()
                .map(UserMapper::toDto)
                .toList();
    }
}
