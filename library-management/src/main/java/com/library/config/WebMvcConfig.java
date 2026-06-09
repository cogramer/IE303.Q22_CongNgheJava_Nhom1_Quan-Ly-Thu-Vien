package com.library.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path bookImageDir = Paths.get("src", "main", "resources", "static", "img", "books")
        .toAbsolutePath()
        .normalize();
    registry.addResourceHandler("/img/books/**")
        .addResourceLocations(bookImageDir.toUri().toString());
  }
}
