package ru.practicum.shareit.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryImplTest {

    private UserRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UserRepositoryImpl();
    }

    private User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    @Test
    void save_newUser_assignsIncrementingIds() {
        User first = repository.save(user("Alice", "alice@mail.com"));
        User second = repository.save(user("Bob", "bob@mail.com"));

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(second.getId()).isEqualTo(2L);
    }

    @Test
    void save_existingUser_keepsIdAndUpdatesData() {
        User saved = repository.save(user("Alice", "alice@mail.com"));
        Long id = saved.getId();

        saved.setName("Alice Updated");
        saved.setEmail("new@mail.com");
        User result = repository.save(saved);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Alice Updated");
        assertThat(result.getEmail()).isEqualTo("new@mail.com");
        assertThat(repository.findById(id)).isSameAs(saved);
    }

    @Test
    void findById_missingUser_returnsNull() {
        assertThat(repository.findById(999L)).isNull();
    }

    @Test
    void findById_existingUser_returnsUser() {
        User saved = repository.save(user("Alice", "alice@mail.com"));

        assertThat(repository.findById(saved.getId())).isSameAs(saved);
    }

    @Test
    void findByEmail_differentCase_returnsUser() {
        repository.save(user("Alice", "alice@mail.com"));

        User found = repository.findByEmail("ALICE@MAIL.COM");

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("alice@mail.com");
    }

    @Test
    void findByEmail_unknownEmail_returnsNull() {
        repository.save(user("Alice", "alice@mail.com"));

        assertThat(repository.findByEmail("unknown@mail.com")).isNull();
    }

    @Test
    void delete_existingUser_removesUser() {
        User saved = repository.save(user("Alice", "alice@mail.com"));

        repository.delete(saved.getId());

        assertThat(repository.findById(saved.getId())).isNull();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findAll_returnsAllUsers() {
        repository.save(user("Alice", "alice@mail.com"));
        repository.save(user("Bob", "bob@mail.com"));

        List<User> all = repository.findAll();

        assertThat(all).hasSize(2)
                .extracting(User::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }
}
