package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.ColorRequest;
import com.sneakershop.backend.dto.product.ColorResponse;
import com.sneakershop.backend.entity.product.Color;
import com.sneakershop.backend.repository.product.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColorService {
    private final ColorRepository colorRepository;

    public List<ColorResponse> getAll() {
        return colorRepository.findAllByDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "CREATE", entity = "Color", description = "Thêm màu: #{#request.name}")
    public ColorResponse create(ColorRequest request) {
        if (colorRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new RuntimeException("Tên màu này đã tồn tại!");
        }
        Color color = new Color();
        color.setName(request.getName());
        color.setHexCode(request.getHexCode());
        color.setDeleted(false);
        return mapToResponse(colorRepository.save(color));
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "UPDATE", entity = "Color", description = "Sửa màu ID #{#id}")
    public ColorResponse update(Long id, ColorRequest request) {
        Color color = colorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        color.setName(request.getName());
        color.setHexCode(request.getHexCode());
        return mapToResponse(colorRepository.save(color));
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "DELETE", entity = "Color", description = "Ẩn màu ID #{#id}")
    public void delete(Long id) {
        Color color = colorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        color.setDeleted(true); // 🔥 Soft Delete (Ẩn)
        colorRepository.save(color);
    }

    private ColorResponse mapToResponse(Color color) {
        ColorResponse res = new ColorResponse();
        res.setId(color.getId());
        res.setName(color.getName());
        res.setHexCode(color.getHexCode());
        return res;
    }
}