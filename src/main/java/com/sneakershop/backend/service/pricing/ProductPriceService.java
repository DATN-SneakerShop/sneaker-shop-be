package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.pricing.PriceBoardDTO;
import com.sneakershop.backend.dto.pricing.PriceHistoryDTO;
import com.sneakershop.backend.dto.pricing.PriceRequest;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.Currency;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import com.sneakershop.backend.entity.product.ProductVariant;

import com.sneakershop.backend.repository.pricing.CurrencyRepository;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.pricing.VariantPriceGroupRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ProductPriceService {

    private final ProductPriceRepository priceRepository;
    private final ProductVariantRepository variantRepository;
    private final CurrencyRepository currencyRepository;
    private final VariantPriceGroupRepository variantPriceGroupRepository;

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
            description = "Cập nhật giá mới cho biến thể giày ID #{#variantId} với mức giá: #{#request.price}")
    public ProductPrice updatePrice(Long variantId, PriceRequest request) {

        // 1. Kiểm tra giá gốc nhập vào phải > 0
        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá gốc phải lớn hơn 0");
        }

        // 2. 🔥 KIỂM TRA QUY TẮC: GIÁ GỐC > MỨC GIẢM NHÓM
        // Lấy tất cả các mức giảm (VIP, THUONG...) của variant này
        List<VariantPriceGroup> groupPrices = variantPriceGroupRepository.findAllByVariant_Id(variantId);

        for (VariantPriceGroup gp : groupPrices) {
            // gp.getPrice() lúc này là "Số tiền giảm"
            if (request.getPrice().compareTo(gp.getPrice()) <= 0) {
                throw new RuntimeException("Lỗi: Giá gốc mới (" + request.getPrice() +
                        ") phải lớn hơn mức giảm đã cài cho nhóm " + gp.getLoaiKhach() +
                        " (Giảm: " + gp.getPrice() + ").");
            }
        }

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy variant"));

        // 3. Vô hiệu hóa giá cũ (Xóa mềm - Soft Delete bằng cách set EndDate)
        priceRepository.findActivePriceForUpdate(variantId).ifPresent(p -> {
            p.setEndDate(LocalDateTime.now());
            p.setDefault(false);
            // Nếu em có trường deleted, hãy set ở đây: p.setDeleted(true);
        });

        // 4. Lưu giá mới
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

    @AuditAction(module = "PRICING", action = "DELETE", entity = "ProductPrice",
            description = "Đã xóa mềm lịch sử giá có ID #{#id}")
    @Transactional
    public void deletePrice(Long id) {
        ProductPrice price = priceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá"));

        if (price.getEndDate() == null && price.isDefault()) {
            throw new RuntimeException("Không được xóa giá đang áp dụng hiện tại. Hãy cập nhật giá mới trước!");
        }

        price.setDeleted(true);
        price.setEndDate(LocalDateTime.now());
        price.setDefault(false);

        priceRepository.save(price);
    }
}
