package com.delight.account.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for serving the frontend React application.
 * Serves static files from frontend/build directory.
 * Note: SPA routing (serving index.html for non-API routes) is handled by FrontendController.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get the absolute path to frontend/build directory
        String userDir = System.getProperty("user.dir");
        String frontendBuildPath = Paths.get(userDir, "frontend", "build").toAbsolutePath().toString();
        logger.info("Configuring static resource handler for frontend build at: {}", frontendBuildPath);
        
        // Verify the build directory exists
        Path buildPath = Paths.get(frontendBuildPath);
        if (!Files.exists(buildPath) || !Files.isDirectory(buildPath)) {
            logger.warn("Frontend build directory does not exist: {}. Static assets will not be served.", frontendBuildPath);
            return;
        }
        
        // Verify assets directory exists
        Path assetsPath = buildPath.resolve("assets");
        if (Files.exists(assetsPath) && Files.isDirectory(assetsPath)) {
            logger.info("Found assets directory at: {}", assetsPath.toAbsolutePath());
        } else {
            logger.warn("Assets directory not found at: {}", assetsPath.toAbsolutePath());
        }
        
        // Serve static assets (JS, CSS, images, etc.) from frontend/build directory
        // The resource location should point to the build directory root
        // Spring will automatically append the request path to this location
        // So /assets/js/index.js will map to frontend/build/assets/js/index.js
        String resourceLocation = "file:" + frontendBuildPath + "/";
        logger.info("Static resource location: {}", resourceLocation);
        
        // Register handler for /assets/** paths
        // IMPORTANT: Spring MVC checks resource handlers BEFORE controllers by default
        // So /assets/** requests should be handled here, not by FrontendController
        // Spring will strip /assets/ prefix and look for the file in the resource location
        // Request: /assets/js/index.js -> resourcePath: "js/index.js" -> file: frontend/build/assets/js/index.js
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("file:" + frontendBuildPath + "/assets/")
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    // resourcePath will be like "js/index.js" (Spring strips /assets/ prefix)
                    // location points to frontend/build/assets/
                    // So we look for: frontend/build/assets/js/index.js
                    Resource resource = location.createRelative(resourcePath);
                    if (resource.exists() && resource.isReadable()) {
                        logger.info("✅ Serving static resource: /assets/" + resourcePath + " from: " + resource.getURI());
                        return resource;
                    } else {
                        logger.error("❌ Static resource not found: /assets/" + resourcePath);
                        logger.error("   Location: " + location);
                        logger.error("   Tried URI: " + resource.getURI());
                        logger.error("   Resource exists: " + resource.exists());
                        logger.error("   Resource readable: " + (resource.exists() ? resource.isReadable() : "N/A"));
                        return null;
                    }
                }
            });
        
        logger.info("✅ Static resource handler configured for /assets/** -> " + resourceLocation + "assets/");
    }
    
    /**
     * Set high priority so resource handlers are registered before controllers.
     * This ensures /assets/** requests are handled by the resource handler, not FrontendController.
     * Note: This affects bean ordering, but handler mapping order is determined by Spring's internal logic.
     * Resource handlers should be checked before controllers by default, but we set this for clarity.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

