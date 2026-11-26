package ru.practicum.shareit.user.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserById() {
        User saved = userRepository.save(user("Alice", "alice@mail.com"));

        assertThat(userRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo("alice@mail.com");
    }

    @Test
    void shouldCheckEmailExistence() {
        userRepository.save(user("Alice", "alice@mail.com"));

        assertThat(userRepository.existsByEmail("alice@mail.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@mail.com")).isFalse();
    }

    @Test
    void shouldFindUsersByNameIgnoringCase() {
        userRepository.save(user("Alice", "alice@mail.com"));
        userRepository.save(user("Bob", "bob@mail.com"));

        List<User> result = userRepository.findByNameContainingIgnoreCase("ALI");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }

    private User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }
}
