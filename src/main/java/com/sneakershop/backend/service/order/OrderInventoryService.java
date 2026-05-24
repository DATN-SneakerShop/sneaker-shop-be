package com.sneakershop.backend.service.order;

import com.sneakershop.backend.entity.inventory.StockTransaction;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.inventory.StockTransactionRepository;
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

    private static final String REF_ORDER = "ORDER";
    private static final String REF_RETURN = "RETURN_REQUEST";

    private final ProductVariantRepository productVariantRepository;
    private final StockTransactionRepository stockTransactionRepository;

    @Transactional
    public void reserveStock(ProductVariant variant, int quantity) {
        if (variant == null) return;
        reserveStock(variant.getId(), quantity, null, null, "Giữ chỗ tồn kho cho đơn hàng.");
    }

    @Transactional
    public void reserveStock(Long variantId, int quantity, String referenceType, Long referenceId, String note) {
        if (variantId == null || quantity <= 0) return;
        ProductVariant managed = findVariantForUpdate(variantId);
        int beforeStock = Math.max(managed.getStock(), 0);
        int beforeReserved = Math.max(managed.getReserved_quantity(), 0);
        int available = beforeStock - beforeReserved;
        if (quantity > available) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng mua vượt quá tồn kho hiện có. Tồn kho còn lại: " + Math.max(available, 0) + "."
            );
        }
        managed.setReserved_quantity(beforeReserved + quantity);
        syncStatus(managed);
        productVariantRepository.save(managed);
        record(managed, "ORDER_RESERVED", quantity, beforeStock, managed.getStock(), beforeReserved, managed.getReserved_quantity(), referenceType, referenceId, note);
    }

    @Transactional
    public void commitReservedStock(Order order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) continue;
            commitReservedStock(item.getVariant().getId(), nz(item.getQuantity()), REF_ORDER, order.getId(), "Hoàn tất đơn hàng, trừ tồn kho đã giữ chỗ.");
        }
    }

    @Transactional
    public void commitReservedStock(Long variantId, int quantity) {
        commitReservedStock(variantId, quantity, null, null, "Hoàn tất đơn hàng, trừ tồn kho đã giữ chỗ.");
    }

    @Transactional
    public void commitReservedStock(Long variantId, int quantity, String referenceType, Long referenceId, String note) {
        if (variantId == null || quantity <= 0) return;
        ProductVariant variant = findVariantForUpdate(variantId);
        int beforeStock = Math.max(variant.getStock(), 0);
        int beforeReserved = Math.max(variant.getReserved_quantity(), 0);
        int commitQty = Math.min(beforeReserved, quantity);
        if (commitQty <= 0) {
            return;
        }
        if (beforeStock < commitQty) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng mua vượt quá tồn kho hiện có. Tồn kho còn lại: " + beforeStock + "."
            );
        }
        variant.setReserved_quantity(beforeReserved - commitQty);
        variant.setStock(beforeStock - commitQty);
        syncStatus(variant);
        productVariantRepository.save(variant);
        record(variant, "ORDER_COMMITTED", -commitQty, beforeStock, variant.getStock(), beforeReserved, variant.getReserved_quantity(), referenceType, referenceId, note);
    }

    @Transactional
    public void releaseForCancellation(Order order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) continue;
            releaseReserved(item.getVariant().getId(), nz(item.getQuantity()), REF_ORDER, order.getId(), "Hủy đơn/giao thất bại, nhả tồn kho đã giữ chỗ.");
        }
    }

    @Transactional
    public void releaseReserved(Long variantId, int quantity, String referenceType, Long referenceId, String note) {
        if (variantId == null || quantity <= 0) return;
        ProductVariant variant = findVariantForUpdate(variantId);
        int beforeStock = Math.max(variant.getStock(), 0);
        int beforeReserved = Math.max(variant.getReserved_quantity(), 0);
        int releaseQty = Math.min(beforeReserved, quantity);
        if (releaseQty <= 0) return;
        variant.setReserved_quantity(beforeReserved - releaseQty);
        syncStatus(variant);
        productVariantRepository.save(variant);
        record(variant, "ORDER_CANCEL_RELEASE", releaseQty, beforeStock, variant.getStock(), beforeReserved, variant.getReserved_quantity(), referenceType, referenceId, note);
    }

    @Transactional
    public void releaseOrRestock(Long variantId, int quantity) {
        releaseReserved(variantId, quantity, null, null, "Nhả tồn kho đã giữ chỗ.");
    }

    @Transactional
    public void restockReturned(Long variantId, int quantity, Long returnRequestId, String note) {
        restock(variantId, quantity, REF_RETURN, returnRequestId, note == null ? "Nhập lại kho từ đơn hoàn trả." : note);
    }

    @Transactional
    public void restock(Long variantId, int quantity, String referenceType, Long referenceId, String note) {
        if (variantId == null || quantity <= 0) return;
        ProductVariant variant = findVariantForUpdate(variantId);
        int beforeStock = Math.max(variant.getStock(), 0);
        int beforeReserved = Math.max(variant.getReserved_quantity(), 0);
        variant.setStock(beforeStock + quantity);
        syncStatus(variant);
        productVariantRepository.save(variant);
        record(variant, "RETURN_RESTOCK", quantity, beforeStock, variant.getStock(), beforeReserved, variant.getReserved_quantity(), referenceType, referenceId, note);
    }

    @Transactional
    public void restockReturnedItems(Order order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) continue;
            int returnedQty = nz(item.getReturnedQuantity());
            if (returnedQty <= 0) continue;
            restock(item.getVariant().getId(), returnedQty, REF_ORDER, order.getId(), "Nhập lại kho từ xử lý hoàn trả cũ.");
        }
    }

    public int getAvailableQuantity(ProductVariant variant) {
        if (variant == null) return 0;
        return Math.max(0, Math.max(variant.getStock(), 0) - Math.max(variant.getReserved_quantity(), 0));
    }

    private ProductVariant findVariantForUpdate(Long variantId) {
        return productVariantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể sản phẩm: " + variantId));
    }

    private void syncStatus(ProductVariant variant) {
        int available = getAvailableQuantity(variant);
        if (available <= 0) {
            variant.setStatus("Hết hàng");
        } else if (Objects.equals(variant.getStatus(), "Hết hàng")) {
            variant.setStatus("Còn hàng");
        }
    }

    private void record(ProductVariant variant, String type, int quantity, int beforeStock, int afterStock,
                        int beforeReserved, int afterReserved, String referenceType, Long referenceId, String note) {
        StockTransaction tx = new StockTransaction();
        tx.setVariant(variant);
        tx.setType(type);
        tx.setQuantity(quantity);
        tx.setBeforeStock(beforeStock);
        tx.setAfterStock(afterStock);
        tx.setBeforeReservedQuantity(beforeReserved);
        tx.setAfterReservedQuantity(afterReserved);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        tx.setNote(note);
        stockTransactionRepository.save(tx);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
