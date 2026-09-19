package ru.practicum.stat.server.util.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.time.format.DateTimeFormatter;

@SpringBootConfiguration
public class StatServerConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        return builder -> {
            builder.simpleDateFormat(pattern);
            builder.serializers(new LocalDateTimeSerializer(
                    DateTimeFormatter.ofPattern(pattern)
            ));
            builder.deserializers(new LocalDateTimeDeserializer(
                    DateTimeFormatter.ofPattern(pattern)
            ));
        };
    }

}