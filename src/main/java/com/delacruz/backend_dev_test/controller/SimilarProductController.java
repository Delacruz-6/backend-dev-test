package com.delacruz.backend_dev_test.controller;

import com.delacruz.backend_dev_test.model.ProductDetail;
import com.delacruz.backend_dev_test.service.SimilarProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class SimilarProductController {

    private final SimilarProductService similarProductService;

    @GetMapping("/{productId}/similar")
    public Mono<ResponseEntity<List<ProductDetail>>> getSimilarProducts(@PathVariable String productId) {
        return similarProductService.getSimilarProducts(productId)
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorResume(WebClientResponseException.NotFound.class, ex ->
                        Mono.just(ResponseEntity.<List<ProductDetail>>notFound().build()))
                .onErrorResume(WebClientRequestException.class, ex -> {
                    log.error("Timeout o error de conexión con el mock: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.<List<ProductDetail>>status(HttpStatus.GATEWAY_TIMEOUT).build());
                });
    }
}
