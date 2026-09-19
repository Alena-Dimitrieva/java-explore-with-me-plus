package ru.practicum.event.util.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class EventServiceConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        return builder -> {
            builder.simpleDateFormat(DATE_TIME_PATTERN);
            builder.serializers(
                    new LocalDateTimeSerializer(formatter)
            );
            builder.deserializers(
                    new LocalDateTimeDeserializer(formatter)
            );
        };
    }
}