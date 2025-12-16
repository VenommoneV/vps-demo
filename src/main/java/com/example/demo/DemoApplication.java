package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@SpringBootApplication
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        // 改个颜色，加个版本号，看看效果
        return "<h1 style='color:blue'>Version 2.0 Released!</h1>" + 
               "<p>Deployed automatically by GitHub Actions.</p>";
    }
}
