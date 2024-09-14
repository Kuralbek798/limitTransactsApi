package com.example.limittransactsapi.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.IOException;


@Slf4j
@Service
public class HttpClientServiceUtil {

   // @Value("${http.client.connectTimeout:10}") // Значение по умолчанию 10 секунд
    private int connectTimeout=5;

  //  @Value("${http.client.requestTimeout:10}") // Значение по умолчанию 10 секунд
    private int requestTimeout =5;

    private final HttpClient httpClient;

    public HttpClientServiceUtil() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(connectTimeout)) // Устанавливаем таймаут подключения
                .build();
    }

    public String sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(requestTimeout)) // Устанавливаем таймаут запроса
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) { // Проверяем код статуса
            log.warn("Request to {} failed with status code {}", url, response.statusCode());
            return null;
        }

        return response.body();
    }
}

