package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.SoleRequest; // 🔥 Dùng SoleRequest
import com.sneakershop.backend.dto.product.SoleResponse;
import com.sneakershop.backend.entity.product.Sole; // 🔥 Dùng Entity Sole
import com.sneakershop.backend.repository.product.SoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class SoleService {
    private final SoleRepository soleRepository; // Đổi tên cho đỡ nhầm

    public List<SoleResponse> getAll() {
        return soleRepository.findAllByDeletedFalse().stream().map(this::mapToRes).toList();
    }

    @Transactional @AuditAction(module="PRODUCT", action="CREATE", entity="Sole", description="Thêm loại đế: #{#req.name}")
    public SoleResponse create(SoleRequest req) {
        if (soleRepository.existsByNameAndDeletedFalse(req.getName())) throw new RuntimeException("Loại đế đã tồn tại!");
        Sole s = new Sole();
        s.setName(req.getName());
        s.setDeleted(false);
        return mapToRes(soleRepository.save(s));
    }

    @Transactional @AuditAction(module="PRODUCT", action="UPDATE", entity="Sole", description="Sửa loại đế ID #{#id}")
    public SoleResponse update(Long id, SoleRequest req) {
        Sole s = soleRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy loại đế"));
        s.setName(req.getName());
        return mapToRes(soleRepository.save(s));
    }

    @Transactional @AuditAction(module="PRODUCT", action="DELETE", entity="Sole", description="Ẩn loại đế ID #{#id}")
    public void delete(Long id) {
        Sole s = soleRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy loại đế"));
        s.setDeleted(true);
        soleRepository.save(s);
    }

    private SoleResponse mapToRes(Sole s) {
        SoleResponse r = new SoleResponse();
        r.setId(s.getId());
        r.setName(s.getName());
        return r;
    }
}