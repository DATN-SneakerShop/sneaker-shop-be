package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.entity.pricing.Currency;
import com.sneakershop.backend.repository.pricing.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;


    // ✅ Tạo tiền tệ
    @Transactional
    public Currency create(Currency currency) {

        if (currencyRepository.existsByCode(currency.getCode())) {
            throw new RuntimeException("Mã tiền tệ đã tồn tại!");
        }

        // nếu set mặc định → bỏ default cũ
        if (currency.isDefault()) {
            currencyRepository.clearDefault();
        }

        return currencyRepository.save(currency);
    }

}
