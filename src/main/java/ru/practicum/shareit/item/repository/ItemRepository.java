package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByOwnerIdOrderByIdAsc(Long ownerId);

    @Query("""
            SELECT i FROM Item i
            WHERE i.available = true
              AND (lower(i.name) LIKE lower(concat('%', :text, '%'))
                OR lower(i.description) LIKE lower(concat('%', :text, '%')))
            """)
    List<Item> search(@Param("text") String text);

    long countByOwnerId(Long ownerId);
}
