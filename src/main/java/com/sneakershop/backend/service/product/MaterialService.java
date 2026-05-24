package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.MaterialRequest;
import com.sneakershop.backend.dto.product.MaterialResponse;
import com.sneakershop.backend.entity.product.Material;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.repository.product.MaterialRepository;
import com.sneakershop.backend.service.ValidationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class MaterialService {
    private final MaterialRepository materialRepository;

    public List<MaterialResponse> getAll() { return materialRepository.findAllByDeletedFalse().stream().map(this::mapToRes).toList(); }

    @Transactional @AuditAction(module="PRODUCT", action="CREATE", entity="Material", description="Thêm chất liệu: #{#req.name}")
    public MaterialResponse create(MaterialRequest req) {
        validateName(req.getName(), null);
        Material m = new Material();
        m.setName(ValidationSupport.trim(req.getName()));
        m.setDeleted(false);
        return mapToRes(materialRepository.save(m));
    }

    @Transactional @AuditAction(module="PRODUCT", action="UPDATE", entity="Material", description="Sửa chất liệu ID #{#id}")
    public MaterialResponse update(Long id, MaterialRequest req) {
        Material m = materialRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu"));
        validateName(req.getName(), id);
        m.setName(ValidationSupport.trim(req.getName()));
        return mapToRes(materialRepository.save(m));
    }

    @Transactional @AuditAction(module="PRODUCT", action="DELETE", entity="Material", description="Ẩn chất liệu ID #{#id}")
    public void delete(Long id) {
        Material m = materialRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu"));
        m.setDeleted(true);
        materialRepository.save(m);
    }

    private void validateName(String rawName, Long currentId) {
        String name = ValidationSupport.trim(rawName);
        if (name == null) throw new ValidationException("name", "Tên không được để trống.");
        boolean duplicate = currentId == null ? materialRepository.existsByNameNormalized(name) : materialRepository.existsByNameNormalizedAndIdNot(name, currentId);
        if (duplicate) throw new ValidationException("name", "Tên chất liệu đã tồn tại.");
    }
    private MaterialResponse mapToRes(Material m) { MaterialResponse r = new MaterialResponse(); r.setId(m.getId()); r.setName(m.getName()); return r; }
}
