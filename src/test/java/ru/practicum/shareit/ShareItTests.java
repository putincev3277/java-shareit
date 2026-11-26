package ru.practicum.shareit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShareItTests {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteBookingAndCommentFlow() throws Exception {
        long ownerId = createUser("Owner", "owner@mail.com");
        long bookerId = createUser("Booker", "booker@mail.com");
        long itemId = createItem(ownerId);
        long bookingId = createBooking(bookerId, itemId);

        approveBooking(ownerId, bookingId);
        addComment(bookerId, itemId);

        mockMvc.perform(get("/items/{itemId}", itemId)
                        .header(USER_HEADER, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBooking.id").value(bookingId))
                .andExpect(jsonPath("$.comments[0].authorName").value("Booker"));

        mockMvc.perform(get("/items/{itemId}", itemId)
                        .header(USER_HEADER, bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBooking").isEmpty())
                .andExpect(jsonPath("$.comments[0].authorName").value("Booker"));

        mockMvc.perform(get("/items")
                        .header(USER_HEADER, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastBooking.id").value(bookingId));
    }

    private long createUser(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "email", email))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private long createItem(long ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/items")
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Drill",
                                "description", "Good drill",
                                "available", true))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private long createBooking(long bookerId, long itemId) throws Exception {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        MvcResult result = mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "itemId", itemId,
                                "start", start.toString(),
                                "end", end.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn();
        return json(result).get("id").asLong();
    }

    private void approveBooking(long ownerId, long bookingId) throws Exception {
        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header(USER_HEADER, ownerId)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private void addComment(long bookerId, long itemId) throws Exception {
        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .header(USER_HEADER, bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "Good item"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value("Booker"));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
