package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.entity.pricing.TienTe;
import com.sneakershop.backend.repository.pricing.TienTeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TienTeService {

    private final TienTeRepository tienTeRepository;
    // lay tien viet lam mac dinh
    public TienTe layTienTeMacDinh() {
        return tienTeRepository.findByLaMacDinhTrue()
                .orElseThrow(() ->
                        new RuntimeException("Chưa thiết lập tiền tệ mặc định"));
    }
}
