package com.cashier.common.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Web MVC 配置
 *
 * @author cashier
 * @since 2024-01-01
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String uploadResourceLocation;

    public WebMvcConfig(@Value("${app.upload-dir}") String uploadDir) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uri = root.toUri().toString();
        this.uploadResourceLocation = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传文件（需先于 /** 注册）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadResourceLocation)
                .setCachePeriod(3600);
        // Knife4j 静态资源
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        // 前端构建产物（Maven 将 cashier-frontend/dist 复制到 classpath:/static/）
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (resourcePath != null
                                && (resourcePath.startsWith("api/")
                                        || "api".equals(resourcePath))) {
                            return null;
                        }
                        Resource resource = super.getResource(resourcePath, location);
                        if (resource != null) {
                            return resource;
                        }
                        if (resourcePath != null && !resourcePath.contains(".")) {
                            Resource index = new ClassPathResource("static/index.html");
                            if (index.exists()) {
                                return index;
                            }
                        }
                        return null;
                    }
                });
    }
}
