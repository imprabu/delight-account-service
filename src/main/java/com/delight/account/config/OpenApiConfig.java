package com.delight.account.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Delight Account Service API",
        version = "2.0.0",
        description = "Comprehensive REST API for managing organizations, accounts, user memberships, authentication, and session management. " +
                     "This service provides secure endpoints for:\n" +
                     "• Organization signup and account creation\n" +
                     "• User authentication (signin/signout) with JWT tokens\n" +
                     "• Account activation and user onboarding\n" +
                     "• Session validation and management\n" +
                     "• Membership management with role-based access control using role IDs\n" +
                     "• Multi-endpoint support (regular, API, and channel endpoints)\n\n" +
                     "Role-Based Access Control:\n" +
                     "• Default system roles: Org Admin (ID: 1), Account Admin (ID: 2), Agent (ID: 3)\n" +
                     "• Custom roles can be created based on default roles\n" +
                     "• Roles are referenced by ID in API requests (e.g., Update Member uses roleId)\n" +
                     "• Privilege-based permissions system with role-privilege assignments\n\n" +
                     "Authentication:\n" +
                     "• Cookie-based (session) and JWT Bearer token authentication are supported for user endpoints.\n" +
                     "• Channel endpoints under '/channel/**' require a ChannelJWT (different from user JWT tokens).\n" +
                     "• Channel endpoints include: /channel/allAccounts (requires channel auth only), /channel/validateSession, /channel/accounts/info\n" +
                     "• Use the Swagger 'Authorize' button with a ChannelJWT to try channel endpoints.\n\n" +
                     "Important Changes:\n" +
                     "• The /accounts/allAccounts endpoint has been removed. Use /channel/allAccounts with channel authentication instead.\n" +
                     "• Role system now uses role IDs instead of role enums. Update API calls to use roleId parameter.",
        termsOfService = "https://delight.example/terms",
        contact = @Contact(
            name = "Delight API Support Team",
            email = "api-support@delight.example",
            url = "https://delight.example/support"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Local development server (default)"
        ),
        @Server(
            url = "http://127.0.0.1:8080",
            description = "Local development server (IP address)"
        ),
        @Server(
            url = "https://staging-api.delight.example",
            description = "Staging environment"
        ),
        @Server(
            url = "https://api.delight.example",
            description = "Production server"
        )
    },
    externalDocs = @ExternalDocumentation(
        description = "Delight Account Service Documentation & API Guide",
        url = "https://docs.delight.example/account-service"
    ),
    security = {
        @SecurityRequirement(name = "bearerAuth"),
        @SecurityRequirement(name = "cookieAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "JWT Bearer token authentication. Include the token in the Authorization header as 'Bearer {token}'. " +
                 "Tokens are obtained from the signin endpoint and contain user, account, and role information."
)
@SecurityScheme(
    name = "cookieAuth",
    type = SecuritySchemeType.APIKEY,
    in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE,
    paramName = "hrsession",
    description = "Session cookie authentication. The session cookie is automatically set after successful signin " +
                 "and contains the JWT token. This method is preferred for web applications."
)
public class OpenApiConfig {
}

