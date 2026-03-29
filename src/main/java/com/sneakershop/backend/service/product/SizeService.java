package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.SizeResponse;
import com.sneakershop.backend.repository.product.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SizeService {
    private final SizeRepository sizeRepository;

    public List<SizeResponse> getAll() {
        return sizeRepository.findAll().stream().map(s -> {
            SizeResponse res = new SizeResponse();
            res.setId(s.getId());
            res.setName(s.getName());
            return res;
        }).collect(Collectors.toList());
    }
}