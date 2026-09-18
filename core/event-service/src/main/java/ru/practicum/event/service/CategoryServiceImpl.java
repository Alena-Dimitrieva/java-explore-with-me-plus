package ru.practicum.event.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dao.CategoryRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.dto.NewCategoryDto;
import ru.practicum.event.mapper.CategoryMapper;
import ru.practicum.event.model.Category;
import ru.practicum.event.util.error.exception.ConflictException;
import ru.practicum.event.util.error.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(
		level = AccessLevel.PRIVATE,
		makeFinal = true
)
public class CategoryServiceImpl
		implements CategoryService {

	CategoryRepository categoryRepository;
	EventRepository eventRepository;

	@Override
	@Transactional
	public CategoryDto adminAddNewCategory(
			@NonNull NewCategoryDto newCategoryDto) {

		if (categoryRepository.existsByName(
				newCategoryDto.name())) {

			throw new ConflictException(
					"Категория с именем '"
							+ newCategoryDto.name()
							+ "' уже существует"
			);
		}

		Category category =
				CategoryMapper.toEntity(newCategoryDto);

		return CategoryMapper.toDto(
				categoryRepository.save(category)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryDto> findAll(
			Integer from,
			Integer size) {

		int page = from / size;

		return categoryRepository
				.findAll(
						PageRequest.of(page, size)
				)
				.stream()
				.map(CategoryMapper::toDto)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryDto findById(Long catId) {

		Category category =
				getCategoryById(catId);

		return CategoryMapper.toDto(category);
	}

	@Override
	@Transactional
	public CategoryDto updateCategory(
			Long catId,
			@NonNull CategoryDto categoryDto) {

		Category category =
				getCategoryById(catId);

		if (!category.getName()
				.equals(categoryDto.name())
				&& categoryRepository.existsByName(
				categoryDto.name())) {

			throw new ConflictException(
					"Категория с именем '"
							+ categoryDto.name()
							+ "' уже существует"
			);
		}

		category.setName(categoryDto.name());

		return CategoryMapper.toDto(
				categoryRepository.save(category)
		);
	}

	@Override
	@Transactional
	public void deleteCategory(Long catId) {

		Category category =
				getCategoryById(catId);

		if (eventRepository.existsByCategoryId(catId)) {
			throw new ConflictException(
					"The category is not empty"
			);
		}

		categoryRepository.delete(category);
	}

	private Category getCategoryById(
			long categoryId) {

		return categoryRepository.findById(categoryId)
				.orElseThrow(
						() -> new NotFoundException(
								"Категория с id="
										+ categoryId
										+ " не найдена"
						)
				);
	}
}