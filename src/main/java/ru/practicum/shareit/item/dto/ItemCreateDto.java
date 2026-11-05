package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateDto {
    @NotBlank(message = "Название вещи обязательно")
    private String name;

    @NotBlank(message = "Описание вещи обязательно")
    private String description;

    @NotNull(message = "Поле available обязательно для заполнения")
    private Boolean available;
}
