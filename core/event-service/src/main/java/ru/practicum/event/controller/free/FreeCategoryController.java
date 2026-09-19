package ru.practicum.event.controller.free;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.service.CategoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
public class FreeCategoryController {

    private final CategoryService categoryService;

    /**
     * Получение категорий.
     * <p>
     * В случае, если по заданным фильтрам не найдено ни одной категории, возвращает пустой список
     *
     * @param from количество категорий, которые нужно пропустить для формирования текущего набора
     *             Default value : 0
     * @param size количество категорий в наборе
     *             Default value : 10
     * @return List<{ @ link CategoryDto }>
     */
    @GetMapping
    public List<CategoryDto> findAll(@RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                     @RequestParam(defaultValue = "10") @Positive Integer size) {
        return categoryService.findAll(from, size);
    }

    /**
     * Получение информации о категории по её идентификатору.
     * <p>
     * В случае, если категории с заданным id не найдено, возвращает статус код 404
     *
     * @param catId id категории
     * @return {@link CategoryDto}
     */
    @GetMapping("/{catId}")
    public CategoryDto findById(@PathVariable @Positive Long catId) {
        return categoryService.findById(catId);
    }
}
