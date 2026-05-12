package com.delacruz.backend_dev_test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.delacruz.backend_dev_test.model.ProductDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarProductService {

    private final WebClient mockApiWebClient;

    public Flux<String> getSimilarIds(String productId) {
        return mockApiWebClient.get()
                .uri("/product/{id}/similarids", productId)
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .map(JsonNode::asText);
    }

    public Mono<ProductDetail> getProductDetail(String productId) {
        return mockApiWebClient.get()
                .uri("/product/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDetail.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("Producto {} no encontrado en el mock, se omite del resultado", productId);
                        return Mono.empty();
                    }
                    log.error("Error al obtener detalle del producto {}: {}", productId, ex.getMessage());
                    return Mono.empty();
                });
    }

    public Flux<ProductDetail> getSimilarProducts(String productId) {
        return getSimilarIds(productId)
                .flatMap(this::getProductDetail);
    }
}
