package ru.practicum.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.event.dao.CategoryRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.dto.NewCategoryDto;
import ru.practicum.event.model.Category;
import ru.practicum.event.util.error.exception.ConflictException;
import ru.practicum.event.util.error.exception.NotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl service;

    @Test
    void addNewCategory_shouldSaveCategory() {
        NewCategoryDto request = new NewCategoryDto("Sport");
        Category saved = Category.builder().name("Sport").build();
        saved.setId(1L);

        when(categoryRepository.existsByName("Sport")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryDto result = service.adminAddNewCategory(request);

        assertEquals(1L, result.id());
        assertEquals("Sport", result.name());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void addNewCategory_shouldThrowConflictWhenNameExists() {
        when(categoryRepository.existsByName("Sport")).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.adminAddNewCategory(new NewCategoryDto("Sport"))
        );

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void findById_shouldReturnCategory() {
        Category category = Category.builder().name("Sport").build();
        category.setId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryDto result = service.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("Sport", result.name());
    }

    @Test
    void findById_shouldThrowNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.findById(999L)
        );
    }

    @Test
    void updateCategory_shouldChangeName() {
        Category category = Category.builder().name("Old").build();
        category.setId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("New")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryDto result = service.updateCategory(
                1L,
                CategoryDto.builder().id(1L).name("New").build()
        );

        assertEquals("New", result.name());
        verify(categoryRepository).save(category);
    }

    @Test
    void deleteCategory_shouldThrowConflictWhenEventsExist() {
        Category category = Category.builder().name("Sport").build();
        category.setId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.deleteCategory(1L)
        );

        verify(categoryRepository, never()).delete(any());
    }
}

