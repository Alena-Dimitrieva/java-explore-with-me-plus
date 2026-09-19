package ru.practicum.stat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {
    @NotBlank(message = "Название приложения не может быть пустым")
    @Size(max = 255, message = "APP слишком длинный (макс. 255 символов)")
    private String app;

    @NotBlank(message = "URI не может быть пустым")
    @Size(max = 512, message = "URI слишком длинный (макс. 512 символов)")
    private String uri;

    @NotBlank(message = "IP-адрес не может быть пустым")
    @Size(max = 45, message = "IP слишком длинный (макс. 45 символов)")
    private String ip;

    @NotNull(message = "TIMESTAMP является обязательным")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Builder.Default
    private String actionType = "VIEW"; // VIEW, LIKE, DISLIKE для рейтингов
}
