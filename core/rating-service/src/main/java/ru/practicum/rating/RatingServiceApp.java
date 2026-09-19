package ru.practicum.rating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {
        "ru.practicum.user.client",
        "ru.practicum.event.client"
})
@SpringBootApplication
public class RatingServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(
                RatingServiceApp.class,
                args
        );
    }
}
