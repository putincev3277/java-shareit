package ru.practicum.shareit.item.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.now().withNano(0);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User author;
    private Item item;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(user("Owner", "owner@mail.com"));
        author = userRepository.save(user("Author", "author@mail.com"));
        item = itemRepository.save(item(owner));
    }

    @Test
    void shouldFindCommentsByItemWithAuthorOrderedByCreatedDesc() {
        Comment oldComment = comment("Old", NOW.minusHours(2));
        Comment newComment = comment("New", NOW.minusHours(1));
        commentRepository.saveAll(List.of(oldComment, newComment));
        entityManager.flush();
        entityManager.clear();

        List<Comment> result = commentRepository.findByItemIdWithAuthorOrderByCreatedDesc(item.getId());

        assertThat(result).extracting(Comment::getText).containsExactly("New", "Old");
        assertThat(result).allSatisfy(comment ->
                assertThat(comment.getAuthor().getName()).isEqualTo("Author"));
    }

    @Test
    void shouldFindCommentsByItemIdsWithAuthorOrderedByCreatedDesc() {
        Comment oldComment = comment("Old", NOW.minusHours(2));
        Comment newComment = comment("New", NOW.minusHours(1));
        commentRepository.saveAll(List.of(oldComment, newComment));
        entityManager.flush();
        entityManager.clear();

        List<Comment> result = commentRepository.findByItemIdInWithAuthorOrderByCreatedDesc(
                List.of(item.getId()));

        assertThat(result).extracting(Comment::getText).containsExactly("New", "Old");
        assertThat(result).allSatisfy(comment ->
                assertThat(comment.getAuthor().getName()).isEqualTo("Author"));
    }

    private User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Item item(User owner) {
        Item item = new Item();
        item.setOwner(owner);
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(true);
        return item;
    }

    private Comment comment(String text, LocalDateTime created) {
        Comment comment = new Comment();
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setText(text);
        comment.setCreated(created);
        return comment;
    }
}
