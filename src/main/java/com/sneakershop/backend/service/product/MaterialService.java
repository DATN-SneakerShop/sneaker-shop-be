package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.MaterialResponse;
import com.sneakershop.backend.repository.product.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {
    private final MaterialRepository materialRepository;

    public List<MaterialResponse> getAll() {
        return materialRepository.findAll().stream().map(m -> {
            MaterialResponse res = new MaterialResponse();
            res.setId(m.getId());
            res.setName(m.getName());
            return res;
        }).collect(Collectors.toList());
    }
}