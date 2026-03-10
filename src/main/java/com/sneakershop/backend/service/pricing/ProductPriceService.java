package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.pricing.PriceBoardDTO;
import com.sneakershop.backend.dto.pricing.PriceHistoryDTO;
import com.sneakershop.backend.dto.pricing.PriceRequest;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.Currency;
import com.sneakershop.backend.entity.product.ProductVariant;

import com.sneakershop.backend.repository.pricing.CurrencyRepository;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductPriceService {

    private final ProductPriceRepository priceRepository;
    private final ProductVariantRepository variantRepository;
    private final CurrencyRepository currencyRepository;

    // ✅ Bảng giá hiện tại
    public List<PriceBoardDTO> getPriceBoard() {
        return priceRepository.getCurrentPriceBoard();
    }

    // ✅ Lịch sử giá theo variant
    public List<PriceHistoryDTO> getPriceHistoryByVariant(Long variantId) {
        return priceRepository.getPriceHistoryByVariant(variantId);
    }


    @Transactional
    @AuditAction(module = "PRICING", action = "UPDATE", entity = "ProductPrice",
            description = "Đã cập nhật giá mới cho biến thể giày ID #{#variantId} với mức giá: #{#request.price}")
    public ProductPrice updatePrice(Long variantId, PriceRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy variant"));

        // 🔒 Đóng giá đang active (lock để tránh ghi đè)
        priceRepository.findActivePriceForUpdate(variantId).ifPresent(p -> {
            p.setEndDate(LocalDateTime.now());
            p.setDefault(false);
        });

        // 🔥 Lấy tiền tệ mặc định nếu không truyền lên
        Currency currency = request.getCurrencyId() == null
                ? currencyRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new RuntimeException("Chưa thiết lập tiền tệ mặc định"))
                : currencyRepository.findById(request.getCurrencyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tiền tệ"));

        ProductPrice newPrice = new ProductPrice();
        newPrice.setVariant(variant);
        newPrice.setPrice(request.getPrice());
        newPrice.setCurrency(currency);
        newPrice.setStartDate(LocalDateTime.now());
        newPrice.setDefault(true);

        return priceRepository.save(newPrice);
    }

    // ✅ Xóa giá (chỉ cho phép xóa giá lịch sử)
    @AuditAction(module = "PRICING", action = "DELETE", entity = "ProductPrice",
            description = "Đã xóa lịch sử giá có ID #{#id}")
    public void deletePrice(Long id) {
        ProductPrice price = priceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá"));

        if (price.getEndDate() == null) {
            throw new RuntimeException("Không được xóa giá đang áp dụng");
        }
        priceRepository.delete(price);
    }
}
