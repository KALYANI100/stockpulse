package com.example.stockpulse.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTests {
    @Test
    void stockChangesPreservePendingReviewUntilResolved() {
        Product product = new Product("SKU-1", "Test item", Category.HOME,
                new BigDecimal("20.00"), 8, 5);
        product.markReviewPending();

        product.updateStock(7);
        assertEquals(ProductStatus.PRICE_REVIEW_PENDING, product.getStatus());

        product.recordSale(1);
        assertEquals(ProductStatus.PRICE_REVIEW_PENDING, product.getStatus());
    }

    @Test
    void saleCannotExceedAvailableStock() {
        Product product = new Product("SKU-2", "Test item", Category.HOME,
                new BigDecimal("20.00"), 1, 5);

        assertThrows(IllegalArgumentException.class, () -> product.recordSale(2));
        assertEquals(1, product.getStockLevel());
    }

    @Test
    void demandVelocityAndThresholdCanBeEditedDownward() {
        Product product = new Product("SKU-3", "Editable item", Category.HOME,
                new BigDecimal("20.00"), 8, 28);
        product.updateDemandVelocity(5);
        product.updateReorderThreshold(3);

        assertEquals(5, product.getDemandVelocity());
        assertEquals(3, product.getReorderThreshold());
        assertThrows(IllegalArgumentException.class, () -> product.updateDemandVelocity(-1));
        assertThrows(IllegalArgumentException.class, () -> product.updateReorderThreshold(-1));
    }
}
