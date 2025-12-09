package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.InetAddress;
import java.time.LocalDateTime;

@SpringBootApplication
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            return "<h1>君君和爷爷!</h1>"; 
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
