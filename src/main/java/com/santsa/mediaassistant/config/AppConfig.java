package com.santsa.mediaassistant.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public RestClient openFdaClient() {
        return RestClient.builder()
                .baseUrl("https://api.fda.gov")
                .build();
    }

}
