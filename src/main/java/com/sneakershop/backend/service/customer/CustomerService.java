package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.entity.customer.*;
import com.sneakershop.backend.repository.customer.*;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final JavaMailSender mailSender;
    private final CustomerRepository repository;
    private final CustomerAuditLogService auditLogService;
    private final CustomerRankHistoryRepository rankHistoryRepo;
    private final CustomerPointHistoryRepository pointHistoryRepo;
    private final CustomerAuditLogRepository auditLogRepo;

    public List<Customer> getAllActive() {
        return repository.findByStatusOrderByDiemTichLuyDesc("ACTIVE");
    }

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

        // ✅ check email null-safe
//        if (kh.getEmail() != null && repository.existsByEmail(kh.getEmail())) {
//            throw new RuntimeException("Email đã tồn tại");
//        }

        // EMAIL
        if (kh.getEmail() != null && kh.getEmail().isBlank()) {
            kh.setEmail(null);
        }

        if (kh.getEmail() != null && !kh.getEmail().isBlank()) {
            if (repository.existsByEmail(kh.getEmail())) {
                throw new RuntimeException("Email đã tồn tại");
            }
        }

// PHONE
        if (kh.getPhone() != null && !kh.getPhone().isBlank()) {
            if (repository.existsByPhone(kh.getPhone())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }
        }


        kh.setStatus("ACTIVE");
        kh.setDiemTichLuy(kh.getDiemTichLuy() != null ? kh.getDiemTichLuy() : 0);
        kh.setLoaiKhach(calculateRank(kh.getDiemTichLuy()));

        kh.setUuDaiTheoDiem(discountByPoint(kh.getDiemTichLuy()));
        kh.setUuDaiTheoNhom(discountByGroup(kh.getLoaiKhach()));

        Customer saved = repository.save(kh);

        auditLogService.log(saved.getId(), "CREATE", "Thêm khách hàng", "ADMIN", "127.0.0.1");

        return saved;
    }

    @Transactional
    public Customer update(Long id, Customer data) {

        Customer kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // ✅ check email an toàn
//        if (data.getEmail() != null &&
//                kh.getEmail() != null &&
//                !kh.getEmail().equalsIgnoreCase(data.getEmail()) &&
//                repository.existsByEmail(data.getEmail())) {
//
//            throw new RuntimeException("Email đã tồn tại");
//        }

        // EMAIL
        if (data.getEmail() != null && !data.getEmail().isBlank()) {

            if (kh.getEmail() == null || !kh.getEmail().equalsIgnoreCase(data.getEmail())) {

                if (repository.existsByEmail(data.getEmail())) {
                    throw new RuntimeException("Email đã tồn tại");
                }
            }
        }

// PHONE

        if (data.getPhone() != null && !data.getPhone().isBlank()) {

            if (kh.getPhone() == null || !kh.getPhone().equals(data.getPhone())) {

                if (repository.existsByPhone(data.getPhone())) {
                    throw new RuntimeException("Số điện thoại đã tồn tại");
                }
            }
        }

        kh.setTen(data.getTen());
        kh.setEmail(data.getEmail());
        kh.setPhone(data.getPhone()); // 🔥 SĐT
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

        kh.setUuDaiTheoDiem(discountByPoint(kh.getDiemTichLuy()));
        kh.setUuDaiTheoNhom(discountByGroup(kh.getLoaiKhach()));

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

            sendRankUpMail(kh, oldRank, newRank); // 🔥 gửi mail
        }

        kh.setUuDaiTheoDiem(discountByPoint(kh.getDiemTichLuy()));
        kh.setUuDaiTheoNhom(discountByGroup(kh.getLoaiKhach()));
    }

    private String calculateRank(int diem) {
        if (diem >= 20000) return "VIP";
        if (diem >= 5000) return "LOYALTY";
        return "NORMAL";
    }

    public void delete(Long id) {
        Customer kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));

        kh.setStatus("INACTIVE");
        repository.save(kh);
    }

    public List<Customer> filter(String loaiKhach, Integer inactiveDays) {

        // CASE: LÂU KHÔNG HOẠT ĐỘNG
        if ("INACTIVE".equalsIgnoreCase(loaiKhach) && inactiveDays != null) {

            LocalDateTime date = LocalDateTime.now().minusDays(inactiveDays);

            return repository.findInactiveCustomers(date);
        }

        // CASE: TẤT CẢ
        if (loaiKhach == null || loaiKhach.equalsIgnoreCase("ALL")) {
            return getAllActive();
        }

        // CASE: VIP / NORMAL / LOYALTY
        return repository.findByStatusAndLoaiKhach("ACTIVE", loaiKhach);
    }

    private int discountByPoint(int diem) {
        if (diem > 40000) return 10;
        if (diem > 12000) return 5;
        return 0;
    }

    private int discountByGroup(String group) {
        switch (group) {
            case "VIP": return 10;
            case "LOYALTY": return 5;
            default: return 0;
        }
    }

    private void validateAge(LocalDate ngaySinh) {
        if (ngaySinh == null) {
            throw new RuntimeException("Ngày sinh không được để trống");
        }

        if (ngaySinh.plusYears(16).isAfter(LocalDate.now())) {
            throw new RuntimeException("Phải đủ 16 tuổi");
        }
    }

    private void validateCustomer(Customer kh) {

        if (kh.getTen() == null || kh.getTen().trim().isEmpty()) {
            throw new RuntimeException("Tên không được để trống");
        }

        if (!kh.getTen().matches("^[a-zA-ZÀ-ỹ\\s]+$")) {
            throw new RuntimeException("Tên không hợp lệ");
        }

        if (kh.getDiemTichLuy() != null && kh.getDiemTichLuy() < 0) {
            throw new RuntimeException("Điểm không hợp lệ");
        }

        // 🔥 validate SĐT
        if (kh.getPhone() != null && !kh.getPhone().isBlank()) {
            if (!kh.getPhone().matches("0\\d{9}")) {
                throw new RuntimeException("SĐT không hợp lệ");
            }
        }

        validateAge(kh.getNgaySinh());
    }

    public Customer findByEmail(String email){
        return repository.findByEmail(email).orElse(null);
    }

    // 🔥 MAIL (AN TOÀN)
    private void sendRankUpMail(Customer c, String oldRank, String newRank) {

        try {

            if (c.getEmail() == null || c.getEmail().isBlank()) {
                System.out.println("Không có email → bỏ qua: " + c.getTen());
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(c.getEmail());
            message.setSubject("🎉 Bạn đã được nâng hạng!");
            message.setText(
                    "Chúc mừng " + c.getTen() + "\n\n" +
                            "Từ " + oldRank + " → " + newRank + "\n" +
                            "Ưu đãi: " + discountByGroup(newRank) + "%"
            );

            mailSender.send(message);

            System.out.println("Đã gửi mail: " + c.getEmail());

        } catch (Exception e) {
            System.out.println("Lỗi mail: " + e.getMessage());
        }
    }

    // Tìm khách theo tên, sđt, email
    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActive();
        }
        return repository.search(keyword);
    }
}