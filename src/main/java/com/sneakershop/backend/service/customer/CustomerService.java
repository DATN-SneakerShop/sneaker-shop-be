package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.*;
import com.sneakershop.backend.repository.customer.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerAuditLogService auditLogService;
    private final CustomerRankHistoryRepository rankHistoryRepo;
    private final CustomerPointHistoryRepository pointHistoryRepo;
    private final CustomerAuditLogRepository auditLogRepo;

    public List<Customer> getAllActive() {
        return repository.findByStatusOrderByDiemTichLuyDesc("ACTIVE");
    }

    // ✅ HÀM DỌN SẠCH DATABASE
    @Transactional
    public void deleteAllCustomers() {
        rankHistoryRepo.deleteAll();
        pointHistoryRepo.deleteAll();
        auditLogRepo.deleteAll();
        repository.deleteAll();
    }

    @Transactional
    public Customer create(Customer kh) {
        if (repository.existsByEmail(kh.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng trong hệ thống, vui lòng kiểm tra lại!");
        }
        kh.setStatus("ACTIVE");
        kh.setDiemTichLuy(kh.getDiemTichLuy() != null ? kh.getDiemTichLuy() : 0);
        kh.setLoaiKhach(calculateRank(kh.getDiemTichLuy()));

        Customer saved = repository.save(kh);
        auditLogService.log(saved.getId(), "CREATE", "Thêm khách hàng mới", "ADMIN", "127.0.0.1");
        return saved;
    }

    @Transactional
    public Customer update(Long id, Customer data) {
        Customer kh = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (!kh.getEmail().equalsIgnoreCase(data.getEmail()) && repository.existsByEmail(data.getEmail())) {
            throw new RuntimeException("Email mới này đã tồn tại trên hệ thống!");
        }

        kh.setTen(data.getTen());
        kh.setEmail(data.getEmail());
        kh.setNgaySinh(data.getNgaySinh());
        kh.setGhiChu(data.getGhiChu());

        Integer oldPoint = kh.getDiemTichLuy();
        Integer newPoint = data.getDiemTichLuy() != null ? data.getDiemTichLuy() : 0;

        if (!oldPoint.equals(newPoint)) {
            // Lưu lịch sử điểm
            CustomerPointHistory ph = new CustomerPointHistory();
            ph.setCustomerId(kh.getId());
            ph.setOldPoint(oldPoint);
            ph.setNewPoint(newPoint);
            ph.setReason("ADMIN UPDATE");
            pointHistoryRepo.save(ph);

            kh.setDiemTichLuy(newPoint);
            // Kiểm tra và lưu lịch sử hạng (Nếu có thay đổi)
            updateRankHistory(kh);
        }

        return repository.save(kh);
    }

    private void updateRankHistory(Customer kh) {
        String oldRank = kh.getLoaiKhach();
        String newRank = calculateRank(kh.getDiemTichLuy());
        if (!newRank.equals(oldRank)) {
            CustomerRankHistory rh = new CustomerRankHistory();
            rh.setCustomerId(kh.getId());
            rh.setOldRank(oldRank);
            rh.setNewRank(newRank);
            rh.setReason("Cập nhật theo điểm");
            rankHistoryRepo.save(rh);
            kh.setLoaiKhach(newRank);
        }
    }

    private String calculateRank(int diem) {
        if (diem >= 20000) return "VIP";
        if (diem >= 5000) return "LOYALTY";
        return "NORMAL";
    }

    public void delete(Long id) {
        Customer kh = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        kh.setStatus("INACTIVE");
        repository.save(kh);
    }

    public List<Customer> filterByLoai(String loaiKhach) {
        if (loaiKhach == null || loaiKhach.equalsIgnoreCase("ALL")) return getAllActive();
        return repository.findByStatusAndLoaiKhach("ACTIVE", loaiKhach);
    }
}