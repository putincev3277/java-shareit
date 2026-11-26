package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ItemDto addItem(Long ownerId, ItemCreateDto dto) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Владелец с ID " + ownerId + " не найден"));

        Item item = new Item();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setAvailable(dto.getAvailable());
        item.setOwner(owner);

        Item saved = itemRepository.save(item);
        return ItemMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, Long userId, ItemUpdateDto dto) {
        Item existing = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с ID " + itemId + " не найдена"));

        if (!existing.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Обновлять вещь может только её владелец");
        }

        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getAvailable() != null) {
            existing.setAvailable(dto.getAvailable());
        }

        Item updated = itemRepository.save(existing);
        return ItemMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemWithBookingsDto getItemWithDetails(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с ID " + itemId + " не найдена"));

        List<Booking> allBookings = bookingRepository.findByItemId(itemId);
        LocalDateTime now = LocalDateTime.now();

        Booking lastBookingEntity = allBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.REJECTED)
                .filter(b -> b.getEnd().isBefore(now))
                .sorted((b1, b2) -> b2.getEnd().compareTo(b1.getEnd()))
                .findFirst()
                .orElse(null);

        Booking nextBookingEntity = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .filter(b -> b.getStart().isAfter(now))
                .sorted(Comparator.comparing(Booking::getStart))
                .findFirst()
                .orElse(null);

        BookingShortDto lastBookingDto = lastBookingEntity != null
                ? BookingMapper.toShortDto(lastBookingEntity)
                : null;

        BookingShortDto nextBookingDto = nextBookingEntity != null
                ? BookingMapper.toShortDto(nextBookingEntity)
                : null;

        List<Comment> comments = commentRepository.findByItemIdWithAuthorOrderByCreatedDesc(itemId);
        List<CommentDto> commentDtos = comments.stream()
                .map(CommentMapper::toDto)
                .toList();

        boolean isOwner = item.getOwner().getId().equals(userId);

        return new ItemWithBookingsDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.isAvailable(),
                item.getOwner().getId(),
                null,
                isOwner ? lastBookingDto : null,
                isOwner ? nextBookingDto : null,
                commentDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getOwnerItemsWithBookingDates(Long ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Владелец с ID " + ownerId + " не найден"));

        List<Item> items = itemRepository.findByOwnerIdOrderByIdAsc(ownerId);
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = bookingRepository
                .findByItemIdInAndStatusNot(itemIds, BookingStatus.REJECTED);

        Map<Long, Booking> lastBookings = new HashMap<>();
        Map<Long, Booking> nextBookings = new HashMap<>();

        for (Booking booking : bookings) {
            Long itemId = booking.getItem().getId();
            if (booking.getEnd().isBefore(now)) {
                lastBookings.merge(itemId, booking,
                        (oldBooking, newBooking) ->
                                newBooking.getEnd().isAfter(oldBooking.getEnd())
                                        ? newBooking : oldBooking);
            } else if (booking.getStatus() == BookingStatus.APPROVED
                    && booking.getStart().isAfter(now)) {
                nextBookings.merge(itemId, booking,
                        (oldBooking, newBooking) ->
                                newBooking.getStart().isBefore(oldBooking.getStart())
                                        ? newBooking : oldBooking);
            }
        }

        Map<Long, List<CommentDto>> commentsByItem = getCommentsForItems(itemIds);

        List<ItemDto> result = new ArrayList<>(items.size());
        for (Item item : items) {
            boolean isOwner = item.getOwner().getId().equals(ownerId);
            Booking lastBooking = lastBookings.get(item.getId());
            Booking nextBooking = nextBookings.get(item.getId());

            result.add(ItemMapper.toDto(
                    item,
                    commentsByItem.getOrDefault(item.getId(), List.of()),
                    isOwner && lastBooking != null
                            ? BookingMapper.toShortDto(lastBooking) : null,
                    isOwner && nextBooking != null
                            ? BookingMapper.toShortDto(nextBooking) : null
            ));
        }
        return result;
    }

    @Override
    @Transactional
    public CommentDto addComment(Long itemId, Long userId, CommentCreateDto dto) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с ID " + itemId + " не найдена"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        boolean hasValidBooking = bookingRepository.existsByItemIdAndBookerIdAndEndBefore(
                itemId, userId, LocalDateTime.now()
        );

        if (!hasValidBooking) {
            throw new IllegalArgumentException(
                    "Оставить отзыв может только пользователь, у которого было завершённое бронирование этой вещи."
            );
        }

        Comment comment = new Comment();
        comment.setItem(item);
        comment.setAuthor(user);
        comment.setText(dto.getText());
        comment.setCreated(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        return CommentMapper.toDto(saved);
    }

    private Map<Long, List<CommentDto>> getCommentsForItems(List<Long> itemIds) {
        List<Comment> comments = commentRepository.findByItemIdInWithAuthorOrderByCreatedDesc(itemIds);

        Map<Long, List<CommentDto>> byItem = new HashMap<>();
        for (Comment comment : comments) {
            byItem.computeIfAbsent(comment.getItem().getId(), k -> new ArrayList<>())
                    .add(CommentMapper.toDto(comment));
        }
        return byItem;
    }
}
