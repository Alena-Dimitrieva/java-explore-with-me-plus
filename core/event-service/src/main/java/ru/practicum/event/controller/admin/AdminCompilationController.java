package ru.practicum.event.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.compilation.CompilationDto;
import ru.practicum.event.dto.compilation.CompilationUpdateDto;
import ru.practicum.event.dto.compilation.NewCompilationDto;
import ru.practicum.event.service.compilation.CompilationService;


@RestController
@RequestMapping(path = "/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {

    private final CompilationService compilationService;

    /**
     * Добавление новой подборки (подборка может не содержать событий)
     *
     * @param newCompilationDto данные новой подборки
     * @return {@link CompilationDto}
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        return compilationService.addCompilation(newCompilationDto);
    }

    /**
     * @param compilationUpdateDto данные для обновления подборки
     * @param compId               id подборки
     * @return {@link CompilationDto}
     */
    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(@RequestBody @Valid CompilationUpdateDto compilationUpdateDto,
                                            @PathVariable Long compId) {
        return compilationService.updateCompilation(compId, compilationUpdateDto);
    }

    /**
     * Удаление подборки
     *
     * @param compId id подборки
     */
    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delById(@PathVariable Long compId) {
        compilationService.delById(compId);
    }
}
