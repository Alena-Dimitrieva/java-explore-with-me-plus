package ru.practicum.event.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.dto.NewCategoryDto;
import ru.practicum.event.service.CategoryService;


@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

	private final CategoryService categoryService;

	/**
	 * Добавление новой категории. Обратите внимание: имя категории должно быть уникальным
	 *
	 * @param newCategoryDto данные добавляемой категории
	 * @return CategoryDto
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryDto addNewCategory(@RequestBody @Valid NewCategoryDto newCategoryDto) {
		return categoryService.adminAddNewCategory(newCategoryDto);
	}

	/**
	 * Изменение категории
	 * <p>
	 * Обратите внимание: имя категории должно быть уникальным
	 *
	 * @param catId       id категории
	 * @param categoryDto Данные категории для изменения
	 * @return CategoryDto
	 */
	@PatchMapping("/{catId}")
	public CategoryDto updateCategory(@PathVariable Long catId,
	                                  @RequestBody @Valid CategoryDto categoryDto) {
		return categoryService.updateCategory(catId, categoryDto);
	}

	/**
	 * Удаление категории
	 * <p>
	 * Обратите внимание: с категорией не должно быть связано ни одного события.
	 *
	 * @param catId id категории
	 */
	@DeleteMapping("/{catId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCategory(@PathVariable Long catId) {
		categoryService.deleteCategory(catId);
	}
}
