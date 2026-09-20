package ru.practicum.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {
        "ru.practicum.user.client",
        "ru.practicum.request.client",
        "ru.practicum.event.client"
})
@SpringBootApplication
public class RequestServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(
                RequestServiceApp.class,
                args
        );
    }
}
