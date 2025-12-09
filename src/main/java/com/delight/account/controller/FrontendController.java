package com.delight.account.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller to serve the frontend React application.
 * This handles SPA routing by serving index.html for all non-API routes.
 * 
 * Note: This controller should have lower priority than API controllers.
 * Spring will match API routes first, and only if no match is found,
 * this catch-all will serve the frontend.
 */
@RestController
public class FrontendController {

    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);

    /**
     * Catch-all handler for frontend routes.
     * Serves index.html for any path that doesn't match API endpoints.
     * This allows React Router to handle client-side routing.
     * 
     * Excludes:
     * - /api/** (API endpoints)
     * - /accounts/** (Account API endpoints)
     * - /channel/** (Channel API endpoints)
     * - /swagger-ui/** (Swagger UI)
     * - /v3/api-docs/** (OpenAPI docs)
     * - /actuator/** (Actuator endpoints)
     * - /test/** (Test endpoints)
     * - Static assets (handled by WebMvcConfig)
     */
    @RequestMapping(value = {"/", "/**"})
    public ResponseEntity<Resource> serveFrontend(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        
        // Explicitly skip /assets/** paths - these are handled by WebMvcConfig resource handler
        // Return 404 so Spring doesn't try to serve index.html for assets
        // The resource handler should have already been checked, but if not, this prevents wrong content
        if (requestPath.startsWith("/assets/")) {
            logger.debug("Skipping /assets/** path in FrontendController: {}", requestPath);
            return ResponseEntity.notFound().build();
        }
        
        // Skip if this is an API endpoint
        if (isApiPath(requestPath)) {
            return ResponseEntity.notFound().build();
        }
        
        // Skip static assets (but allow index.html to be served)
        if (isStaticAsset(requestPath) && !requestPath.equals("/index.html") && !requestPath.equals("/")) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            // Try multiple possible paths for the frontend build
            Path indexHtmlPath = null;
            String[] possiblePaths = {
                "frontend/build/index.html",  // Relative from project root
                System.getProperty("user.dir") + "/frontend/build/index.html",  // Absolute from working directory
            };
            
            for (String pathStr : possiblePaths) {
                Path path = Paths.get(pathStr);
                if (Files.exists(path)) {
                    indexHtmlPath = path;
                    break;
                }
            }
            
            // If still not found, try to resolve relative to current working directory
            if (indexHtmlPath == null) {
                Path relativePath = Paths.get(System.getProperty("user.dir"), "frontend", "build", "index.html");
                if (Files.exists(relativePath)) {
                    indexHtmlPath = relativePath;
                }
            }
            
            if (indexHtmlPath == null || !Files.exists(indexHtmlPath)) {
                logger.warn("Frontend build not found. Tried paths: {}. Please run './gradlew npmBuild' to build the frontend.", 
                           String.join(", ", possiblePaths));
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(indexHtmlPath.toFile());
            
            logger.debug("Serving frontend index.html for path: {} from: {}", requestPath, indexHtmlPath.toAbsolutePath());
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(resource);
        } catch (Exception e) {
            logger.error("Error serving frontend for path {}: {}", requestPath, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if the path is an API endpoint that should be handled by controllers.
     */
    private boolean isApiPath(String path) {
        return path.startsWith("/api/") ||
               path.startsWith("/accounts/") ||
               path.startsWith("/channel/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator") ||
               path.startsWith("/test/");
    }

    /**
     * Check if the path is a static asset (has a file extension).
     */
    private boolean isStaticAsset(String path) {
        return path.contains(".") && 
               (path.endsWith(".js") || path.endsWith(".css") || 
                path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".svg") || path.endsWith(".ico") ||
                path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") ||
                path.endsWith(".eot") || path.endsWith(".json") || path.endsWith(".map"));
    }
}

