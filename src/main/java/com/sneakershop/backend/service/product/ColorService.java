package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.ColorRequest;
import com.sneakershop.backend.dto.product.ColorResponse;
import com.sneakershop.backend.entity.product.Color;
import com.sneakershop.backend.repository.product.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    @AuditAction(module = "PRODUCT", action = "CREATE", entity = "Color",
            description = "Đã thêm mới màu sắc: #{#request.name}")
    public ColorResponse create(ColorRequest request) {
        Color color = new Color();
        color.setName(request.getName());
        color.setHexCode(request.getHexCode());
        colorRepository.save(color);
        return mapToResponse(color);
    }

    public List<ColorResponse> getAll() {
        return colorRepository.findAllByOrderByIdDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "UPDATE", entity = "Color",
            description = "Đã cập nhật màu ID #{#id} thành: #{#request.name}")
    public ColorResponse update(Long id, ColorRequest request) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Color not found: " + id));

        color.setName(request.getName());
        color.setHexCode(request.getHexCode());
        return mapToResponse(color);
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "DELETE", entity = "Color",
            description = "Đã xóa màu ID #{#id}")
    public void delete(Long id) {
        if (!colorRepository.existsById(id)) {
            throw new EntityNotFoundException("Color not found: " + id);
        }
        colorRepository.deleteById(id);
    }

    private ColorResponse mapToResponse(Color color) {
        ColorResponse res = new ColorResponse();
        res.setId(color.getId());
        res.setName(color.getName());
        res.setHexCode(color.getHexCode());
        return res;
    }
}