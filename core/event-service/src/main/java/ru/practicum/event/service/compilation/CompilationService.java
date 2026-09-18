package ru.practicum.event.service.compilation;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.event.dto.compilation.CompilationDto;
import ru.practicum.event.dto.compilation.CompilationSearchFilter;
import ru.practicum.event.dto.compilation.CompilationUpdateDto;
import ru.practicum.event.dto.compilation.NewCompilationDto;


import java.util.List;

public interface CompilationService {

	CompilationDto getById(Long compilationId, HttpServletRequest request);

	void delById(Long compilationId);

	CompilationDto addCompilation(NewCompilationDto compilation);

	CompilationDto updateCompilation(Long compilationId, CompilationUpdateDto compilation);

	List<CompilationDto> getByFilter(CompilationSearchFilter filter, HttpServletRequest request);
}
