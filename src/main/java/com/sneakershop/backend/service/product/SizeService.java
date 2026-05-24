package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.SizeRequest;
import com.sneakershop.backend.dto.product.SizeResponse;
import com.sneakershop.backend.entity.product.Size;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.repository.product.SizeRepository;
import com.sneakershop.backend.service.ValidationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class SizeService {
    private final SizeRepository sizeRepository;
    public List<SizeResponse> getAll() { return sizeRepository.findAllByDeletedFalse().stream().map(this::mapToRes).toList(); }

    @Transactional @AuditAction(module="PRODUCT", action="CREATE", entity="Size", description="Thêm Size: #{#req.name}")
    public SizeResponse create(SizeRequest req) {
        validateName(req.getName(), null);
        Size s = new Size();
        s.setName(ValidationSupport.trim(req.getName()));
        s.setDeleted(false);
        return mapToRes(sizeRepository.save(s));
    }

    @Transactional @AuditAction(module="PRODUCT", action="UPDATE", entity="Size", description="Sửa Size ID #{#id}")
    public SizeResponse update(Long id, SizeRequest req) {
        Size s = sizeRepository.findById(id).orElseThrow();
        validateName(req.getName(), id);
        s.setName(ValidationSupport.trim(req.getName()));
        return mapToRes(sizeRepository.save(s));
    }

    @Transactional @AuditAction(module="PRODUCT", action="DELETE", entity="Size", description="Ẩn Size ID #{#id}")
    public void delete(Long id) {
        Size s = sizeRepository.findById(id).orElseThrow();
        s.setDeleted(true); sizeRepository.save(s);
    }
    private void validateName(String rawName, Long currentId) {
        String name = ValidationSupport.trim(rawName);
        if (name == null) throw new ValidationException("name", "Tên không được để trống.");
        boolean duplicate = currentId == null ? sizeRepository.existsByNameNormalized(name) : sizeRepository.existsByNameNormalizedAndIdNot(name, currentId);
        if (duplicate) throw new ValidationException("name", "Tên size đã tồn tại.");
    }
    private SizeResponse mapToRes(Size s) { SizeResponse r = new SizeResponse(); r.setId(s.getId()); r.setName(s.getName()); return r; }
}
