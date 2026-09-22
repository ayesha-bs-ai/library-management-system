package com.librarymanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final PasswordChangeInterceptor passwordChangeInterceptor;
    private final AppProperties appProperties;

    public WebMvcConfig(PasswordChangeInterceptor passwordChangeInterceptor, AppProperties appProperties) {
        this.passwordChangeInterceptor = passwordChangeInterceptor;
        this.appProperties = appProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passwordChangeInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = appProperties.getUploadDir();
        // Ensure file: prefix and trailing slash
        if (!uploadDir.endsWith("/")) {
            uploadDir = uploadDir + "/";
        }
        if (!uploadDir.startsWith("file:")) {
            uploadDir = "file:" + uploadDir;
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir)
                .setCachePeriod(3600);
    }
}
