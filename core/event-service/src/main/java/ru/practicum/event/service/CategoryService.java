package ru.practicum.event.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    @Transactional
    CategoryDto adminAddNewCategory(NewCategoryDto newCategoryDto);

    List<CategoryDto> findAll(Integer from, Integer size);

    CategoryDto findById(Long catId);

    @Transactional
    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);

    @Transactional
    void deleteCategory(Long catId);
}
