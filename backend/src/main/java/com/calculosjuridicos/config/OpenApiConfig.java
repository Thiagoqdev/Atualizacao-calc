package com.calculosjuridicos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API de Cálculos Jurídicos")
                .version("1.0.0")
                .description("Sistema de Cálculos de Atualização Financeira para o Direito. " +
                            "Permite calcular correção monetária, juros, multas e honorários " +
                            "para processos judiciais.")
                .contact(new Contact()
                    .name("Suporte")
                    .email("suporte@calculosjuridicos.com"))
                .license(new License()
                    .name("Proprietária")
                    .url("https://calculosjuridicos.com/licenca"))
            );
    }
}
