package ru.practicum.shareit.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :nameFragment, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("nameFragment") String nameFragment);

    boolean existsByEmail(String email);
}
