package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.SoleResponse;
import com.sneakershop.backend.repository.product.SoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SoleService {

    private final SoleRepository soleRepository;

    public List<SoleResponse> getAll() {
        return soleRepository.findAll().stream().map(s -> {
            SoleResponse res = new SoleResponse();
            res.setId(s.getId());
            res.setName(s.getName());
            return res;
        }).collect(Collectors.toList());
    }
}