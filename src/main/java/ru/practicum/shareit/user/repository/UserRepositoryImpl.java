package ru.practicum.shareit.user.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<User> findAll() {
        return users.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<User> findByNameContaining(String nameFragment) {
        if (nameFragment == null || nameFragment.isBlank()) {
            return findAll();
        }
        String lower = nameFragment.toLowerCase();
        return users.values().stream()
                .filter(u -> u.getName() != null && u.getName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        users.remove(id);
    }

    @Override
    public User findByEmail(String email) {
        // сравнение без учёта регистра: правило проекта для дубликатов email
        if (email == null) {
            return null;
        }
        String lower = email.toLowerCase();
        return users.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().equals(lower))
                .findFirst()
                .orElse(null);
    }

    @Override
    public User save(User user) {
        Long id = user.getId();
        if (id == null) {
            id = nextId.getAndIncrement();
            user.setId(id);
        }
        users.put(id, user);
        return user;
    }

    @Override
    public User findById(Long id) {
        return users.get(id);
    }
}
