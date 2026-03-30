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

    // 🔥 Inject thêm Repository mới để gọi bảng Hạng Khách Hàng
    private final CustomerRankRepository rankConfigRepository;

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
        // 🔥 NỚI LỎNG: Cho phép ngày sinh, SĐT trống lúc đăng ký
        validateCustomer(kh);

        // Xử lý Email an toàn
        if (kh.getEmail() != null && kh.getEmail().isBlank()) {
            kh.setEmail(null);
        }
        if (kh.getEmail() != null && repository.existsByEmail(kh.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Xử lý Phone an toàn
        if (kh.getPhone() != null && !kh.getPhone().isBlank()) {
            if (repository.existsByPhone(kh.getPhone())) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }
        }

        kh.setStatus("ACTIVE");
        kh.setDiemTichLuy(kh.getDiemTichLuy() != null ? kh.getDiemTichLuy() : 0);
        kh.setLoaiKhach(calculateRank(kh.getDiemTichLuy()));

        // ĐÃ XÓA: uuDaiTheoDiem và uuDaiTheoNhom (Dọn sạch theo Entity mới)

        Customer saved = repository.save(kh);
        auditLogService.log(saved.getId(), "CREATE", "Thêm khách hàng", "ADMIN", "127.0.0.1");

        return saved;
    }

    @Transactional
    public Customer update(Long id, Customer data) {
        Customer kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // Gọi validate bổ sung (nếu khách vào trang cá nhân điền SĐT/Ngày sinh thì check kĩ)
        validateCustomer(data);

        if (data.getEmail() != null && !data.getEmail().isBlank()) {
            if (kh.getEmail() == null || !kh.getEmail().equalsIgnoreCase(data.getEmail())) {
                if (repository.existsByEmail(data.getEmail())) throw new RuntimeException("Email đã tồn tại");
            }
        }
        if (data.getPhone() != null && !data.getPhone().isBlank()) {
            if (kh.getPhone() == null || !kh.getPhone().equals(data.getPhone())) {
                if (repository.existsByPhone(data.getPhone())) throw new RuntimeException("Số điện thoại đã tồn tại");
            }
        }

        kh.setTen(data.getTen());
        kh.setEmail(data.getEmail());
        kh.setPhone(data.getPhone());
        kh.setNgaySinh(data.getNgaySinh());
        kh.setGhiChu(data.getGhiChu());

        Integer oldPoint = kh.getDiemTichLuy();
        Integer newPoint = data.getDiemTichLuy() != null ? data.getDiemTichLuy() : 0;

        if (!oldPoint.equals(newPoint)) {
            CustomerPointHistory ph = new CustomerPointHistory();
            ph.setCustomerId(kh.getId());
            ph.setOldPoint(oldPoint);
            ph.setNewPoint(newPoint);
            ph.setReason("Cập nhật thủ công");
            pointHistoryRepo.save(ph);

            kh.setDiemTichLuy(newPoint);
            updateRankHistory(kh);
        }

        return repository.save(kh);
    }

    // =========================================================================
    // 🔥 CÁC HÀM XỬ LÝ HẠNG VÀ ĐIỂM (CỐT LÕI)
    // =========================================================================

    // HÀM MỚI TẠO: GỌI HÀM NÀY SAU KHI ĐƠN HÀNG THANH TOÁN THÀNH CÔNG
    @Transactional
    public void addPointsFromOrder(Long customerId, double totalOrderAmount) {
        Customer kh = repository.findById(customerId).orElse(null);
        if (kh == null) return;

        // Quy đổi: 10.000 VNĐ = 1 điểm
        int pointsToAdd = (int) (totalOrderAmount / 10000);
        int oldPoint = kh.getDiemTichLuy() != null ? kh.getDiemTichLuy() : 0;
        int newPoint = oldPoint + pointsToAdd;

        CustomerPointHistory ph = new CustomerPointHistory();
        ph.setCustomerId(kh.getId());
        ph.setOldPoint(oldPoint);
        ph.setNewPoint(newPoint);
        ph.setReason("Tích điểm từ đơn hàng mua thành công");
        pointHistoryRepo.save(ph);

        kh.setDiemTichLuy(newPoint);
        updateRankHistory(kh);
        repository.save(kh);
    }

    private void updateRankHistory(Customer kh) {
        String oldRank = kh.getLoaiKhach();
        String newRank = calculateRank(kh.getDiemTichLuy()); // Tính lại rank

        if (!newRank.equals(oldRank)) {
            CustomerRankHistory rh = new CustomerRankHistory();
            rh.setCustomerId(kh.getId());
            rh.setOldRank(oldRank);
            rh.setNewRank(newRank);
            rh.setReason("Tự động thăng hạng theo điểm tích lũy");
            rankHistoryRepo.save(rh);

            kh.setLoaiKhach(newRank);
            sendRankUpMail(kh, oldRank, newRank); // Bắn mail chúc mừng
        }
    }

    // ĐÃ SỬA: Lấy Rank từ Database, quét từ cao xuống thấp thay vì Hardcode
    private String calculateRank(int diem) {
        List<CustomerRank> ranks = rankConfigRepository.findAllByOrderByMinPointsDesc();

        // Trả về mặc định nếu Admin chưa cấu hình gì
        if (ranks.isEmpty()) return "BRONZE";

        for (CustomerRank r : ranks) {
            if (diem >= r.getMinPoints()) {
                return r.getName(); // Đạt mốc nào ăn mốc đó
            }
        }
        // Nếu không đạt mốc nào, lấy rank thấp nhất
        return ranks.get(ranks.size() - 1).getName();
    }

    // Hàm phụ trợ để lấy % giảm giá từ DB chèn vào mail
    private int getDiscountPercentByRank(String rankName) {
        List<CustomerRank> ranks = rankConfigRepository.findAllByOrderByMinPointsDesc();
        for (CustomerRank r : ranks) {
            if (r.getName().equalsIgnoreCase(rankName)) {
                return r.getDiscountPercent() != null ? r.getDiscountPercent() : 0;
            }
        }
        return 0;
    }

    // =========================================================================

    public void delete(Long id) {
        Customer kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        kh.setStatus("INACTIVE");
        repository.save(kh);
    }

    public List<Customer> filter(String loaiKhach, Integer inactiveDays) {
        if ("INACTIVE".equalsIgnoreCase(loaiKhach) && inactiveDays != null) {
            LocalDateTime date = LocalDateTime.now().minusDays(inactiveDays);
            return repository.findInactiveCustomers(date);
        }
        if (loaiKhach == null || loaiKhach.equalsIgnoreCase("ALL")) {
            return getAllActive();
        }
        return repository.findByStatusAndLoaiKhach("ACTIVE", loaiKhach);
    }

    // 🔥 NỚI LỎNG: Nếu ngaySinh bị null thì cho qua, nếu nhập thì check 16 tuổi
    private void validateAge(LocalDate ngaySinh) {
        if (ngaySinh != null && ngaySinh.plusYears(16).isAfter(LocalDate.now())) {
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
        // 🔥 NỚI LỎNG: Nếu SĐT trống thì cho qua, nếu điền thì phải đủ 10 số
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

    private void sendRankUpMail(Customer c, String oldRank, String newRank) {
        try {
            if (c.getEmail() == null || c.getEmail().isBlank()) {
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(c.getEmail());
            message.setSubject("🎉 Chúc mừng bạn đã được nâng hạng thành viên!");
            message.setText(
                    "Chào " + c.getTen() + ",\n\n" +
                            "Hệ thống đã tự động nâng hạng thành viên cho bạn:\n" +
                            "Từ hạng: " + oldRank + " ➡️ Lên hạng: " + newRank + "\n\n" +
                            "Bạn sẽ được giảm trực tiếp " + getDiscountPercentByRank(newRank) + "% cho các đơn hàng tiếp theo.\n\n" +
                            "Cảm ơn bạn đã đồng hành cùng Sneaker Shop!"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Lỗi gửi mail thăng hạng: " + e.getMessage());
        }
    }

    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActive();
        }
        return repository.search(keyword);
    }
}