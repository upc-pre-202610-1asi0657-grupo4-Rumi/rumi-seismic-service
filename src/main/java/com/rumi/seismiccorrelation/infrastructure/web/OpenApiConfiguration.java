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
                .description("REST API of the Seismic Correlation bounded context: seismic events "
                        + "reported by the IGP (US13), risk indexes of buildings (US11) and their affected "
                        + "zones (US12). The service consumes the SensorReadingRecorded event over RabbitMQ "
                        + "and correlates it with the IGP events. Errors use RFC 7807 ProblemDetail.")
                .version("0.1.0"));
    }
}
