package com.rumi.seismiccorrelation.infrastructure.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI seismicServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Rumi Seismic Service API")
                .description("REST API of the Seismic Correlation bounded context: "
                        + "seismic events, risk indexes and reports. No REST endpoint is implemented yet; "
                        + "the service currently consumes the SensorReadingRecorded event over RabbitMQ.")
                .version("0.1.0"));
    }
}
