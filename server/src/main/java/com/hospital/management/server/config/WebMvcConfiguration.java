package com.hospital.management.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File webapp = new File("webapp");
        File parentWebapp = new File("../webapp");

        String webappLocation = webapp.exists() ? "file:" + webapp.getAbsolutePath() + "/" : "file:webapp/";
        String parentWebappLocation = parentWebapp.exists() ? "file:" + parentWebapp.getAbsolutePath() + "/" : "file:../webapp/";

        registry.addResourceHandler("/**")
                .addResourceLocations(
                        webappLocation,
                        parentWebappLocation,
                        "file:webapp/",
                        "file:../webapp/",
                        "classpath:/static/",
                        "classpath:/public/"
                );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}

