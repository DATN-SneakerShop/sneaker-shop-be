package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.*;
import com.sneakershop.backend.repository.customer.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        validateCustomer(kh);
        if (repository.existsByEmail(kh.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng trong hệ thống, vui lòng kiểm tra lại!");
        }
        kh.setStatus("ACTIVE");
        kh.setDiemTichLuy(kh.getDiemTichLuy() != null ? kh.getDiemTichLuy() : 0);
        kh.setLoaiKhach(calculateRank(kh.getDiemTichLuy()));
        kh.setUuDaiTheoDiem(
                discountByPoint(kh.getDiemTichLuy())
        );

        kh.setUuDaiTheoNhom(
                discountByGroup(kh.getLoaiKhach())
        );

        Customer saved = repository.save(kh);
        auditLogService.log(saved.getId(), "CREATE", "Thêm khách hàng mới", "ADMIN", "127.0.0.1");
        return saved;
    }

    @Transactional
    public Customer update(Long id, Customer data) {

        Customer kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (!kh.getEmail().equalsIgnoreCase(data.getEmail())
                && repository.existsByEmail(data.getEmail())) {

            throw new RuntimeException("Email mới này đã tồn tại trên hệ thống!");
        }

        kh.setTen(data.getTen());
        kh.setEmail(data.getEmail());
        kh.setNgaySinh(data.getNgaySinh());
        kh.setGhiChu(data.getGhiChu());

        Integer oldPoint = kh.getDiemTichLuy();
        Integer newPoint = data.getDiemTichLuy() != null ? data.getDiemTichLuy() : 0;

        if (!oldPoint.equals(newPoint)) {

            CustomerPointHistory ph = new CustomerPointHistory();
            ph.setCustomerId(kh.getId());
            ph.setOldPoint(oldPoint);
            ph.setNewPoint(newPoint);
            ph.setReason("ADMIN UPDATE");

            pointHistoryRepo.save(ph);

            kh.setDiemTichLuy(newPoint);

            updateRankHistory(kh);
        }

        // luôn cập nhật ưu đãi
        kh.setUuDaiTheoDiem(
                discountByPoint(kh.getDiemTichLuy())
        );

        kh.setUuDaiTheoNhom(
                discountByGroup(kh.getLoaiKhach())
        );

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

        // luôn cập nhật ưu đãi
        kh.setUuDaiTheoDiem(
                discountByPoint(kh.getDiemTichLuy())
        );

        kh.setUuDaiTheoNhom(
                discountByGroup(kh.getLoaiKhach())
        );
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

    private int discountByPoint(int diem){

        if(diem > 40000){
            return 10;
        }

        if(diem > 12000){
            return 5;
        }

        return 0;
    }

    private int discountByGroup(String group){

        switch(group){

            case "VIP":
                return 10;

            case "LOYALTY":
                return 5;

            default:
                return 0;
        }
    }

    private void validateAge(LocalDate ngaySinh){

        if(ngaySinh == null){
            throw new RuntimeException("Ngày sinh không được để trống");
        }

        LocalDate now = LocalDate.now();

        if(ngaySinh.plusYears(16).isAfter(now)){
            throw new RuntimeException("Khách hàng phải đủ 16 tuổi");
        }
    }

    private void validateCustomer(Customer kh){

        // Tên
        if(kh.getTen() == null || kh.getTen().trim().isEmpty()){
            throw new RuntimeException("Tên không được để trống");
        }

        if(!kh.getTen().matches("^[a-zA-ZÀ-ỹ\\s]+$")){
            throw new RuntimeException("Tên không được chứa số hoặc ký tự đặc biệt");
        }

        // Điểm
        if(kh.getDiemTichLuy() != null && kh.getDiemTichLuy() < 0){
            throw new RuntimeException("Điểm không được âm");
        }

        // Tuổi
        validateAge(kh.getNgaySinh());

    }
}