package com.onesteprest.onesteprest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI oneStepRestOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OneStepRest API")
                        .description("RESTful API generated automatically by OneStepRest framework")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("OneStepRest Team")
                                .url("https://github.com/yourusername/onesteprest")
                                .email("your.email@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}