package com.velox.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.velox.security.AuthUser;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * The API description the web and mobile developers (and the testers) work from: every endpoint, its body and
 * its errors, with a "Authorize" button for the session token. Off in production ({@code VELOX_API_DOCS=false}).
 */
@Configuration
public class OpenApiConfig {

    static {
        // The signed-in account is resolved from the Authorization header, not sent as a parameter.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthUser.class);
    }

    @Bean
    public OpenAPI veloxApi() {
        return new OpenAPI()
                .info(new Info().title("VELOX API").version("2.0")
                        .description("Delivery platform for Samannoud. Errors are always {status, code, message, details}.")
                        .license(new License().name("DEPI team project")))
                .components(new Components().addSecuritySchemes("token", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .description("Token from POST /api/auth/login or /api/auth/register")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
