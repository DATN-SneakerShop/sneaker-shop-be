package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.SoleRequest;
import com.sneakershop.backend.dto.product.SoleResponse;
import com.sneakershop.backend.entity.product.Sole;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.repository.product.SoleRepository;
import com.sneakershop.backend.service.ValidationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class SoleService {
    private final SoleRepository soleRepository;

    public List<SoleResponse> getAll() { return soleRepository.findAllByDeletedFalse().stream().map(this::mapToRes).toList(); }

    @Transactional @AuditAction(module="PRODUCT", action="CREATE", entity="Sole", description="Thêm loại đế: #{#req.name}")
    public SoleResponse create(SoleRequest req) {
        validateName(req.getName(), null);
        Sole s = new Sole();
        s.setName(ValidationSupport.trim(req.getName()));
        s.setDeleted(false);
        return mapToRes(soleRepository.save(s));
    }

    @Transactional @AuditAction(module="PRODUCT", action="UPDATE", entity="Sole", description="Sửa loại đế ID #{#id}")
    public SoleResponse update(Long id, SoleRequest req) {
        Sole s = soleRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy loại đế"));
        validateName(req.getName(), id);
        s.setName(ValidationSupport.trim(req.getName()));
        return mapToRes(soleRepository.save(s));
    }

    @Transactional @AuditAction(module="PRODUCT", action="DELETE", entity="Sole", description="Ẩn loại đế ID #{#id}")
    public void delete(Long id) {
        Sole s = soleRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy loại đế"));
        s.setDeleted(true);
        soleRepository.save(s);
    }

    private void validateName(String rawName, Long currentId) {
        String name = ValidationSupport.trim(rawName);
        if (name == null) throw new ValidationException("name", "Tên không được để trống.");
        boolean duplicate = currentId == null ? soleRepository.existsByNameNormalized(name) : soleRepository.existsByNameNormalizedAndIdNot(name, currentId);
        if (duplicate) throw new ValidationException("name", "Tên đế giày đã tồn tại.");
    }
    private SoleResponse mapToRes(Sole s) { SoleResponse r = new SoleResponse(); r.setId(s.getId()); r.setName(s.getName()); return r; }
}
