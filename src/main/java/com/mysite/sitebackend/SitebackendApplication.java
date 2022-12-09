package com.mysite.sitebackend;

import com.mysite.sitebackend.configurable.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Import(Config.class)
@SpringBootApplication
public class SitebackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SitebackendApplication.class, args);
    }

    // CORS 설정
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/api/**").allowedOrigins("*");
    }
}
