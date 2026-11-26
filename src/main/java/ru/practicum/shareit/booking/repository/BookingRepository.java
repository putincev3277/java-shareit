package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.booker.id = :bookerId " +
            "ORDER BY b.start DESC")
    List<Booking> findByBookerIdOrderByStartDesc(@Param("bookerId") Long bookerId);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.booker.id = :bookerId " +
            "AND b.start <= :now AND b.end >= :now " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByBooker_Id(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.booker.id = :bookerId AND b.end < :now " +
            "ORDER BY b.end DESC")
    List<Booking> findPastByBooker_Id(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.booker.id = :bookerId AND b.start > :now " +
            "ORDER BY b.start DESC")
    List<Booking> findFutureByBooker_Id(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.booker.id = :bookerId AND b.status = :status " +
            "ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndStatusOrderByStartDesc(
            @Param("bookerId") Long bookerId,
            @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.item.owner.id = :ownerId " +
            "ORDER BY b.start DESC")
    List<Booking> findAllByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.start <= :now AND b.end >= :now " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByOwner(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.item.owner.id = :ownerId AND b.end < :now " +
            "ORDER BY b.end DESC")
    List<Booking> findPastByOwner(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.item.owner.id = :ownerId AND b.start > :now " +
            "ORDER BY b.start DESC")
    List<Booking> findFutureByOwner(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "JOIN FETCH b.booker " +
            "WHERE b.item.owner.id = :ownerId AND b.status = :status " +
            "ORDER BY b.start DESC")
    List<Booking> findByOwnerAndStatus(@Param("ownerId") Long ownerId, @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.item " +
            "WHERE b.item.id IN :itemIds AND b.status <> :status")
    List<Booking> findByItemIdInAndStatusNot(
            @Param("itemIds") List<Long> itemIds,
            @Param("status") BookingStatus status);

    boolean existsByItemIdAndBookerIdAndEndBefore(
            Long itemId, Long bookerId, LocalDateTime endBefore);

    List<Booking> findByItemId(Long itemId);
}
