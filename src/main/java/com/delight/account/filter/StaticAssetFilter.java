package com.delight.account.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Filter to serve static assets from frontend/build directory.
 * This runs BEFORE controllers, ensuring /assets/** requests are served directly.
 * 
 * Order is set to run early in the filter chain, before Spring Security and other filters.
 */
@Component
@Order(0) // Run FIRST, before all other filters including TenantFilter
public class StaticAssetFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(StaticAssetFilter.class);
    private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = urlPathHelper.getPathWithinApplication(httpRequest);
        
        // Only handle /assets/** paths
        if (requestPath != null && requestPath.startsWith("/assets/")) {
            try {
                // Get the absolute path to frontend/build directory
                String userDir = System.getProperty("user.dir");
                String frontendBuildPath = Paths.get(userDir, "frontend", "build").toAbsolutePath().toString();
                
                // Map /assets/js/index.js to frontend/build/assets/js/index.js
                String assetPath = requestPath.substring(1); // Remove leading /
                Path filePath = Paths.get(frontendBuildPath, assetPath);
                
                if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                    Resource resource = new FileSystemResource(filePath.toFile());
                    
                    // Set appropriate content type
                    String contentType = getContentType(filePath);
                    httpResponse.setContentType(contentType);
                    httpResponse.setContentLengthLong(Files.size(filePath));
                    httpResponse.setHeader("Cache-Control", "public, max-age=3600");
                    
                    // Copy file content to response
                    Files.copy(filePath, httpResponse.getOutputStream());
                    httpResponse.getOutputStream().flush();
                    
                    logger.debug("Served static asset: {} ({} bytes)", requestPath, Files.size(filePath));
                    return; // Don't continue filter chain
                } else {
                    logger.warn("Static asset not found: {} (tried: {})", requestPath, filePath.toAbsolutePath());
                    httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return; // Don't continue filter chain
                }
            } catch (Exception e) {
                logger.error("Error serving static asset {}: {}", requestPath, e.getMessage(), e);
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return; // Don't continue filter chain
            }
        }
        
        // Not an asset request, continue with filter chain
        chain.doFilter(request, response);
    }
    
    /**
     * Determine content type based on file extension.
     */
    private String getContentType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (fileName.endsWith(".woff")) {
            return "font/woff";
        } else if (fileName.endsWith(".woff2")) {
            return "font/woff2";
        } else if (fileName.endsWith(".ttf")) {
            return "font/ttf";
        } else if (fileName.endsWith(".eot")) {
            return "application/vnd.ms-fontobject";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".map")) {
            return "application/json";
        } else {
            return "application/octet-stream";
        }
    }
}

