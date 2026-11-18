package dev.rafaromero.attendance.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Food Bank Attendance API")
                .version("1.0")
                .description("Beneficiaries, NFC cards and attendances for a food bank. "
                        + "One attendance per person and day, card replacement without losing history, "
                        + "DNI/NIE check-letter validation and RFC 9457 error responses.")
                .license(new License().name("MIT")));
    }
}
