package com.example.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI fintechOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Fintech API")
            .description("Backend APIs for account management and transactions")
            .version("1.0"));
  }
}
