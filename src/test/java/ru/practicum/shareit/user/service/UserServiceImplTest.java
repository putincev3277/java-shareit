package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final String NAME = "Alice";
    private static final String EMAIL = "alice@mail.com";
    private static final String OTHER_EMAIL = "other@mail.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateUserWhenEmailIsUnique() {
        UserCreateDto dto = new UserCreateDto(NAME, EMAIL);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });

        UserDto result = userService.create(dto);

        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void shouldThrowConflictWhenEmailAlreadyExists() {
        UserCreateDto dto = new UserCreateDto(NAME, EMAIL);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.create(dto))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldKeepOldValuesWhenUpdateFieldsAreBlank() {
        User existing = user(USER_ID, NAME, EMAIL);
        UserUpdateDto dto = new UserUpdateDto();
        dto.setName(" ");
        dto.setEmail(" ");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserDto result = userService.updateUser(USER_ID, dto);

        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void shouldUpdateUserWhenFieldsArePresent() {
        User existing = user(USER_ID, NAME, EMAIL);
        UserUpdateDto dto = new UserUpdateDto();
        dto.setName("Alice Updated");
        dto.setEmail(OTHER_EMAIL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail(OTHER_EMAIL)).thenReturn(false);
        when(userRepository.save(existing)).thenReturn(existing);

        UserDto result = userService.updateUser(USER_ID, dto);

        assertThat(result.getName()).isEqualTo("Alice Updated");
        assertThat(result.getEmail()).isEqualTo(OTHER_EMAIL);
    }

    @Test
    void shouldThrowConflictWhenUpdatingToExistingEmail() {
        User existing = user(USER_ID, NAME, EMAIL);
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail(OTHER_EMAIL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail(OTHER_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(USER_ID, dto))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldReturn404WhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturn404WhenDeletingMissingUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    private User user(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }
}
