package dev.rafaromero.attendance.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Food Bank Attendance API")
                        .version("1.1")
                        .description("Beneficiaries, NFC cards and attendances for a food bank. "
                                + "One attendance per person and day, card replacement without losing history, "
                                + "DNI/NIE check-letter validation and RFC 9457 error responses. "
                                + "Requires HTTP Basic credentials: FRONT_DESK for day-to-day work, "
                                + "COORDINATOR for deactivations, renewals, card replacements and reports.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
