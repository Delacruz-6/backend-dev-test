package com.delacruz.backend_dev_test.controller;

import com.delacruz.backend_dev_test.model.ProductDetail;
import com.delacruz.backend_dev_test.service.SimilarProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(SimilarProductController.class)
class SimilarProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SimilarProductService similarProductService;

    @Test
    void getSimilarProducts_returnsOkWithProductList() {
        var products = List.of(
                new ProductDetail("2", "Dress", 19.99, true),
                new ProductDetail("3", "Blazer", 29.99, false),
                new ProductDetail("4", "Boots", 39.99, true)
        );
        when(similarProductService.getSimilarProducts("1"))
                .thenReturn(Flux.fromIterable(products));

        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .hasSize(3)
                .contains(products.toArray(new ProductDetail[0]));
    }

    @Test
    void getSimilarProducts_returnsEmptyList_whenNoSimilarsExist() {
        when(similarProductService.getSimilarProducts("99"))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/product/99/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .hasSize(0);
    }

    @Test
    void getSimilarProducts_whenProductNotFound_returns404() {
        when(similarProductService.getSimilarProducts("999"))
                .thenReturn(Flux.error(WebClientResponseException.create(
                        404, "Not Found", HttpHeaders.EMPTY, new byte[0], null)));

        webTestClient.get()
                .uri("/product/999/similar")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getSimilarProducts_whenConnectionTimeout_returns504() {
        when(similarProductService.getSimilarProducts("1"))
                .thenReturn(Flux.error(new WebClientRequestException(
                        new RuntimeException("Connection timeout"),
                        HttpMethod.GET,
                        URI.create("http://localhost:3001"),
                        HttpHeaders.EMPTY)));

        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }
}
