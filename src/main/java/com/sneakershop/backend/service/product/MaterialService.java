package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction; // 🔥 Thêm import Audit
import com.sneakershop.backend.dto.product.MaterialRequest; // 🔥 Dùng MaterialRequest
import com.sneakershop.backend.dto.product.MaterialResponse;
import com.sneakershop.backend.entity.product.Material; // 🔥 Dùng Entity Material
import com.sneakershop.backend.repository.product.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class MaterialService {
    private final MaterialRepository materialRepository; // Đổi tên cho đúng nghĩa

    public List<MaterialResponse> getAll() {
        return materialRepository.findAllByDeletedFalse().stream().map(this::mapToRes).toList();
    }

    @Transactional @AuditAction(module="PRODUCT", action="CREATE", entity="Material", description="Thêm chất liệu: #{#req.name}")
    public MaterialResponse create(MaterialRequest req) {
        if (materialRepository.existsByNameAndDeletedFalse(req.getName())) throw new RuntimeException("Chất liệu đã tồn tại!");
        Material m = new Material();
        m.setName(req.getName());
        m.setDeleted(false);
        return mapToRes(materialRepository.save(m));
    }

    @Transactional @AuditAction(module="PRODUCT", action="UPDATE", entity="Material", description="Sửa chất liệu ID #{#id}")
    public MaterialResponse update(Long id, MaterialRequest req) {
        Material m = materialRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu"));
        m.setName(req.getName());
        return mapToRes(materialRepository.save(m));
    }

    @Transactional @AuditAction(module="PRODUCT", action="DELETE", entity="Material", description="Ẩn chất liệu ID #{#id}")
    public void delete(Long id) {
        Material m = materialRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu"));
        m.setDeleted(true);
        materialRepository.save(m);
    }

    private MaterialResponse mapToRes(Material m) {
        MaterialResponse r = new MaterialResponse();
        r.setId(m.getId());
        r.setName(m.getName());
        return r;
    }
}