package kz.ibrazaim.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    @Value("${app.upload.banners-dir:uploads/banners}")
    private String bannersUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registerUploadHandler(registry, "/uploads/products/**", uploadDir);
        registerUploadHandler(registry, "/uploads/banners/**", bannersUploadDir);
    }

    private void registerUploadHandler(ResourceHandlerRegistry registry, String urlPattern, String dir) {
        Path uploadPath = Paths.get(dir).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(urlPattern)
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
    }
}

