package com.delacruz.backend_dev_test.service;

import com.delacruz.backend_dev_test.model.ProductDetail;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarProductServiceTest {

    private MockWebServer mockWebServer;
    private SimilarProductService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        service = new SimilarProductService(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // --- getSimilarIds ---

    @Test
    void getSimilarIds_returnsIdsAsStrings() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("[2,3,4]")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(service.getSimilarIds("1"))
                .expectNext("2", "3", "4")
                .verifyComplete();
    }

    @Test
    void getSimilarIds_whenProductNotFound_propagatesNotFoundError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(service.getSimilarIds("999"))
                .expectError(WebClientResponseException.NotFound.class)
                .verify();
    }

    // --- getProductDetail ---

    @Test
    void getProductDetail_returnsFullProductDetail() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true}")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(service.getProductDetail("2"))
                .expectNext(new ProductDetail("2", "Dress", 19.99, true))
                .verifyComplete();
    }

    @Test
    void getProductDetail_whenNotFound_returnsEmptyInsteadOfError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        // Degradación elegante: producto no encontrado se omite (no falla el flujo)
        StepVerifier.create(service.getProductDetail("999"))
                .verifyComplete();
    }

    @Test
    void getProductDetail_whenTimeout_returnsEmptyInsteadOfError() {
        // Respuesta con delay mayor que el timeout configurado (200ms en test)
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":\"1000\",\"name\":\"Coat\",\"price\":89.99,\"availability\":true}")
                .addHeader("Content-Type", "application/json")
                .setHeadersDelay(300, java.util.concurrent.TimeUnit.MILLISECONDS));

        // Usamos un WebClient con timeout muy corto para simular la casuística
        HttpClient fastTimeoutClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(200));
        WebClient shortTimeoutWebClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(fastTimeoutClient))
                .build();
        SimilarProductService serviceWithShortTimeout = new SimilarProductService(shortTimeoutWebClient);

        StepVerifier.create(serviceWithShortTimeout.getProductDetail("1000"))
                .verifyComplete();
    }

    @Test
    void getProductDetail_whenServerError_returnsEmptyInsteadOfError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // Degradación elegante: error del servidor se absorbe, no se propaga
        StepVerifier.create(service.getProductDetail("6"))
                .verifyComplete();
    }

    // --- getSimilarProducts (orquestación completa) ---

    @Test
    void getSimilarProducts_returnsAllProductsWhenAllExist() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("[1,2,3]")
                .addHeader("Content-Type", "application/json"));
        // 3 respuestas de detalle exitosas (orden de llegada concurrente, no garantizado)
        for (int i = 1; i <= 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"" + i + "\",\"name\":\"Product " + i + "\",\"price\":9.99,\"availability\":true}")
                    .addHeader("Content-Type", "application/json"));
        }

        StepVerifier.create(service.getSimilarProducts("X").collectList())
                .assertNext(list -> assertThat(list).hasSize(3))
                .verifyComplete();
    }

    @Test
    void getSimilarProducts_skipsProductsWithNotFoundOrServerError() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("[1,2,3]")
                .addHeader("Content-Type", "application/json"));
        // 1 éxito, 1 no encontrado (404), 1 error de servidor (500)
        // Las peticiones son concurrentes: se verifica el conteo, no el orden
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":\"1\",\"name\":\"Shirt\",\"price\":9.99,\"availability\":true}")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(service.getSimilarProducts("X").collectList())
                .assertNext(list -> assertThat(list).hasSize(1))
                .verifyComplete();
    }

    @Test
    void getSimilarProducts_whenSimilarIdsReturnsEmpty_returnsEmptyList() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(service.getSimilarProducts("X").collectList())
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

    @Test
    void getSimilarProducts_whenSimilarIdsEndpointNotFound_propagatesError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(service.getSimilarProducts("999"))
                .expectError(WebClientResponseException.NotFound.class)
                .verify();
    }
}
