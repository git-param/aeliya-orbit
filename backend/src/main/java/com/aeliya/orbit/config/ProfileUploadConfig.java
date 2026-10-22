package com.aeliya.orbit.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileUploadConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // Stream multipart uploads through the servlet temp directory without an application size cap.
        return new MultipartConfigElement("", -1L, -1L, 0);
    }
}
