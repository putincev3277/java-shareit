package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.model.User;
import java.util.List;

public interface UserRepository {
    User save(User user);

    User findById(Long id);

    List<User> findAll();

    List<User> findByNameContaining(String nameFragment);

    void delete(Long id);

    User findByEmail(String email);
}
