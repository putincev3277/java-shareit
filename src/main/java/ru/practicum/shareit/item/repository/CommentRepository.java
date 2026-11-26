package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "WHERE c.item.id IN :itemIds " +
            "ORDER BY c.created DESC")
    List<Comment> findByItemIdInWithAuthorOrderByCreatedDesc(@Param("itemIds") List<Long> itemIds);

    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "WHERE c.item.id = :itemId " +
            "ORDER BY c.created DESC")
    List<Comment> findByItemIdWithAuthorOrderByCreatedDesc(@Param("itemId") Long itemId);
}
