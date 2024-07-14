package com.example;

import com.example.utils.Utils;
import com.google.common.collect.ImmutableList;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class App implements ApplicationListener<ApplicationReadyEvent> {
    public static void main(String[] args) {
        SpringApplication.run(App.class);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("Hello from Spring Boot built with Declarative Gradle");
    }
}