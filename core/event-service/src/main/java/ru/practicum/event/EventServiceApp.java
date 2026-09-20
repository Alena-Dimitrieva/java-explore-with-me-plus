package ru.practicum.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {
        "ru.practicum.user.client",
        "ru.practicum.request.client",
        "ru.practicum.stat.client"
})
@SpringBootApplication(scanBasePackages = {
        "ru.practicum.event",
        "ru.practicum.user",
        "ru.practicum.request",
        "ru.practicum.stat"
})
public class EventServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApp.class, args);
    }
}