package com.delacruz.backend_dev_test.model;

public record ProductDetail(
        String id,
        String name,
        Double price,
        Boolean availability
) {
}
