package com.sensordata.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${app.sensor-server.url:http://localhost:8080}")
    private String sensorServerUrl;

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider = ConnectionProvider.builder("sensor-client")
                .maxConnections(100)
                .maxIdleTime(java.time.Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(java.time.Duration.ofSeconds(30))
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                );

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        log.info("Creating WebClient with baseUrl={}", sensorServerUrl);

        return WebClient.builder()
                .baseUrl(sensorServerUrl)
                .clientConnector(connector)
                .build();
    }
}
