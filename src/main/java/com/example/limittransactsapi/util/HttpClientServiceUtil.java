package com.example.limittransactsapi.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Slf4j
@Service
public class HttpClientServiceUtil {

    private int connectTimeout = 5;
    private int requestTimeout = 5;
    private final Executor customExecutor;
    private final HttpClient httpClient;

    public HttpClientServiceUtil(@Qualifier("taskExecutor") Executor customExecutor) {
        this.customExecutor = customExecutor;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build();
    }
    @Async("customExecutor")
    public CompletableFuture<String> sendRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(requestTimeout))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) { // Проверяем код статуса
                        log.warn("Request to {} failed with status code {}", url, response.statusCode());
                        return null;
                    }
                    return response.body(); // Возвращаем тело ответа
                })
                .exceptionally(e -> {
                    log.error("Error occurred while sending request to {}: {}", url, e.getMessage());
                    return null;
                });
    }
}
