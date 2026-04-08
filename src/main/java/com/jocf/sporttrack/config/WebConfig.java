package com.jocf.sporttrack.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadRootAbsoluFileUrl;

    public WebConfig(@Value("${sporttrack.profile-upload-path}") String cheminRepertoireUpload) {
        Path absolu = Paths.get(cheminRepertoireUpload).toAbsolutePath().normalize();
        this.uploadRootAbsoluFileUrl = "file:" + absolu + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profiles/**")
                .addResourceLocations(uploadRootAbsoluFileUrl);
    }
}
