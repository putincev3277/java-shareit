package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.user.model.User;

public class CommentMapper {

    public static CommentDto toDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        User author = comment.getAuthor();
        String authorName = author != null ? author.getName() : null;

        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setAuthorName(authorName);
        dto.setCreated(comment.getCreated());
        return dto;
    }
}
