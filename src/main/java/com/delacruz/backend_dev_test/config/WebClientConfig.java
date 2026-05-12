package com.delacruz.backend_dev_test.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${mock.api.base-url}")
    private String baseUrl;

    @Value("${mock.api.connect-timeout-ms}")
    private int connectTimeout;

    @Value("${mock.api.response-timeout-ms}")
    private int responseTimeout;

    @Bean
    public WebClient mockApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(responseTimeout, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
