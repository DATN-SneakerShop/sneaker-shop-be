package com.sneakershop.backend.service.order;

import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderInventoryService {

    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public void reserveStock(ProductVariant variant, int quantity) {
        if (variant == null || quantity <= 0) {
            return;
        }
        ProductVariant managed = findVariant(variant.getId());
        int available = getAvailableQuantity(managed);
        if (quantity > available) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không đủ tồn kho cho SKU " + managed.getSku() + ". Khả dụng: " + available + ", cần: " + quantity
            );
        }
        managed.setReserved_quantity(Math.max(managed.getReserved_quantity(), 0) + quantity);
        productVariantRepository.save(managed);
    }

    @Transactional
    public void commitReservedStock(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) {
                continue;
            }
            commitReservedStock(item.getVariant().getId(), nz(item.getQuantity()));
        }
    }

    @Transactional
    public void commitReservedStock(Long variantId, int quantity) {
        if (variantId == null || quantity <= 0) {
            return;
        }
        ProductVariant variant = findVariant(variantId);
        int reserved = Math.max(variant.getReserved_quantity(), 0);
        if (reserved <= 0) {
            return;
        }

        int commitQty = Math.min(reserved, quantity);
        if (variant.getStock() < commitQty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không đủ tồn kho để hoàn tất đơn cho SKU " + variant.getSku()
            );
        }

        variant.setReserved_quantity(reserved - commitQty);
        variant.setStock(variant.getStock() - commitQty);
        if (variant.getStock() <= 0) {
            variant.setStock(0);
            variant.setStatus("Hết hàng");
        } else if (Objects.equals(variant.getStatus(), "Hết hàng")) {
            variant.setStatus("Còn hàng");
        }
        productVariantRepository.save(variant);
    }

    @Transactional
    public void releaseForCancellation(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) {
                continue;
            }
            releaseOrRestock(item.getVariant().getId(), nz(item.getQuantity()));
        }
    }

    @Transactional
    public void releaseOrRestock(Long variantId, int quantity) {
        if (variantId == null || quantity <= 0) {
            return;
        }
        ProductVariant variant = findVariant(variantId);
        int reserved = Math.max(variant.getReserved_quantity(), 0);
        int releaseQty = Math.min(reserved, quantity);
        int restockQty = quantity - releaseQty;

        if (releaseQty > 0) {
            variant.setReserved_quantity(reserved - releaseQty);
        }
        if (restockQty > 0) {
            variant.setStock(variant.getStock() + restockQty);
        }
        if (variant.getStock() > 0 && Objects.equals(variant.getStatus(), "Hết hàng")) {
            variant.setStatus("Còn hàng");
        }
        productVariantRepository.save(variant);
    }

    @Transactional
    public void restockReturnedItems(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) {
                continue;
            }
            int returnedQty = nz(item.getReturnedQuantity());
            if (returnedQty <= 0) {
                continue;
            }
            ProductVariant variant = findVariant(item.getVariant().getId());
            variant.setStock(variant.getStock() + returnedQty);
            if (variant.getStock() > 0 && Objects.equals(variant.getStatus(), "Hết hàng")) {
                variant.setStatus("Còn hàng");
            }
            productVariantRepository.save(variant);
        }
    }

    public int getAvailableQuantity(ProductVariant variant) {
        if (variant == null) {
            return 0;
        }
        return variant.getStock() - Math.max(variant.getReserved_quantity(), 0);
    }

    private ProductVariant findVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found: " + variantId));
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
