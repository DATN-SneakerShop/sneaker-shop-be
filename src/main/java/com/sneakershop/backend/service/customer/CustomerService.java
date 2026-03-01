package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.customer.CustomerPointHistory;
import com.sneakershop.backend.entity.customer.CustomerRankHistory;
import com.sneakershop.backend.repository.customer.CustomerPointHistoryRepository;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.customer.CustomerRankHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerAuditLogService auditLogService;
    private final CustomerRankHistoryRepository rankHistoryRepo;
    private final CustomerPointHistoryRepository pointHistoryRepo;

    public CustomerService(CustomerRepository repository,
                           CustomerAuditLogService auditLogService,
                           CustomerRankHistoryRepository rankHistoryRepo,
                           CustomerPointHistoryRepository pointHistoryRepo) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.rankHistoryRepo = rankHistoryRepo;
        this.pointHistoryRepo = pointHistoryRepo;
    }

    // =============================
    // LẤY DANH SÁCH ACTIVE
    // =============================
    public List<Customer> getAllActive() {
        return repository.findByStatusOrderByDiemTichLuyDesc("ACTIVE");
    }

    // =============================
    // CREATE
    // =============================
    public Customer create(Customer kh) {

        kh.setStatus("ACTIVE");

        if (kh.getDiemTichLuy() == null) {
            kh.setDiemTichLuy(0);
        }

        // LƯU TRƯỚC
        Customer saved = repository.save(kh);

        // AUTO TÍNH HẠNG
        updateRank(saved, "CREATE CUSTOMER");

        Customer finalSaved = repository.save(saved);

        auditLogService.log(
                finalSaved.getId(),
                "CREATE",
                "Tạo khách hàng: " + finalSaved.getTen(),
                "ADMIN",
                "127.0.0.1"
        );

        return finalSaved;
    }

    // =============================
    // UPDATE
    // =============================
    public Customer update(Long id, Customer data) {

        Customer kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        kh.setTen(data.getTen());
        kh.setEmail(data.getEmail());
        kh.setSoDienThoai(data.getSoDienThoai());
        kh.setNgaySinh(data.getNgaySinh());
        kh.setGhiChu(data.getGhiChu());

        Integer oldPoint = kh.getDiemTichLuy();
        Integer newPoint = data.getDiemTichLuy();

        if (newPoint == null) newPoint = 0;

        if (!oldPoint.equals(newPoint)) {


            CustomerPointHistory history = new CustomerPointHistory();
            history.setCustomerId(kh.getId());
            history.setOldPoint(oldPoint);
            history.setNewPoint(newPoint);
            history.setReason("ADMIN UPDATE");

            pointHistoryRepo.save(history);
        }

        kh.setDiemTichLuy(newPoint);

// Sau đó mới update rank
        updateRank(kh, "ADMIN UPDATE POINTS");

        Customer updated = repository.save(kh);

        auditLogService.log(
                updated.getId(),
                "UPDATE",
                "Cập nhật khách hàng: " + updated.getTen(),
                "ADMIN",
                "127.0.0.1"
        );

        return updated;
    }

    // =============================
    // DELETE (SOFT)
    // =============================
    public void delete(Long id) {

        Customer kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        kh.setStatus("INACTIVE");
        repository.save(kh);

        auditLogService.log(
                kh.getId(),
                "DELETE",
                "Xóa khách hàng: " + kh.getTen(),
                "ADMIN",
                "127.0.0.1"
        );
    }

    // =============================
    // FILTER
    // =============================
    public List<Customer> filterByLoai(String loaiKhach) {
        if (loaiKhach == null || loaiKhach.equalsIgnoreCase("ALL")) {
            return repository.findByStatus("ACTIVE");
        }
        return repository.findByStatusAndLoaiKhach("ACTIVE", loaiKhach);
    }

    // =============================
    // CALCULATE RANK
    // =============================
    private String calculateRank(int diem) {
        if (diem >= 20000) return "VIP";
        if (diem >= 5000) return "LOYALTY";
        return "NORMAL";
    }

    // =============================
    // UPDATE RANK + SAVE HISTORY
    // =============================
    private void updateRank(Customer kh, String reason) {

        String oldRank = kh.getLoaiKhach();
        String newRank = calculateRank(kh.getDiemTichLuy());

        if (oldRank == null || !oldRank.equals(newRank)) {

            CustomerRankHistory history = new CustomerRankHistory();
            history.setCustomerId(kh.getId());
            history.setOldRank(oldRank);
            history.setNewRank(newRank);
            history.setReason(reason);

            rankHistoryRepo.save(history);

            kh.setLoaiKhach(newRank);
        }
    }
}