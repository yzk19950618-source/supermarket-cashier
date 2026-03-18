package com.cashier.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 *
 * @author cashier
 * @since 2024-01-01
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("超市收银系统 API")
                        .description("超市收银系统后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("cashier")
                                .email("cashier@example.com")));
    }
}
