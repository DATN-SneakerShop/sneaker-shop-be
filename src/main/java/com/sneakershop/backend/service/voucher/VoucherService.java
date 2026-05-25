package com.sneakershop.backend.service.voucher;

import com.sneakershop.backend.dto.voucher.CustomerVoucherDTO;
import com.sneakershop.backend.dto.voucher.VoucherRequest;
import com.sneakershop.backend.dto.voucher.VoucherResponse;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.voucher.Voucher;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.service.ValidationSupport;
import com.sneakershop.backend.entity.voucher.VoucherCustomer;
import com.sneakershop.backend.entity.voucher.VoucherUsage;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.voucher.VoucherCustomerRepository;
import com.sneakershop.backend.repository.voucher.VoucherRepository;

import com.sneakershop.backend.repository.voucher.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final VoucherCustomerRepository voucherCustomerRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Transactional
    public void useVoucher(Long voucherId, Long customerId, Long orderId, Double discountAmount) {
        Voucher v = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        // 1. Tăng số lượng đã dùng
        v.setUsedCount((v.getUsedCount() == null ? 0 : v.getUsedCount()) + 1);
        voucherRepository.save(v);

        // 2. Lưu lịch sử sử dụng (Để không cho dùng lại lần 2)
        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(v);
        if (customerId != null) {
            customerRepository.findById(customerId).ifPresent(usage::setCustomer);
        }
        usage.setOrderId(orderId);
        usage.setDiscountAmount(discountAmount);
        usage.setUsedAt(LocalDateTime.now());
        voucherUsageRepository.save(usage);
    }

    // 🔥 Lấy tất cả voucher
    public List<VoucherResponse> getAllVouchers() {
        return voucherRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 🔥 Lọc theo trạng thái
    public List<VoucherResponse> getVouchersByStatus(String status) {
        return voucherRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void saveVoucherCustomers(Long voucherId, List<Long> customerIds) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        // 1. Tìm và xóa sạch khách hàng cũ đã gán cho voucher này
        List<VoucherCustomer> oldMappings = voucherCustomerRepository.findByVoucherId(voucherId);
        voucherCustomerRepository.deleteAll(oldMappings);

        // 2. Nếu là chế độ Riêng tư (có danh sách ID), tiến hành lưu mới
        if (customerIds != null && !customerIds.isEmpty()) {
            for (Long cId : customerIds) {
                customerRepository.findById(cId).ifPresent(customer -> {
                    VoucherCustomer vc = new VoucherCustomer();
                    vc.setVoucher(voucher);
                    vc.setCustomer(customer);
                    voucherCustomerRepository.save(vc);
                });
            }
        }
    }

    // 👉 mapping entity → DTO
    private VoucherResponse mapToResponse(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .name(v.getName())
                .type(v.getType())
                .value(v.getValue())
                .maxDiscount(v.getMaxDiscount())
                .minOrderValue(v.getMinOrderValue())
                .quantity(v.getQuantity())
                .usedCount(v.getUsedCount())
                .status(v.getStatus())
                .isPublic(v.getIsPublic())
                .description(v.getDescription())
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .build();
    }
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        v.setDeleted(true);
        v.setDeletedAt(LocalDateTime.now());

        voucherRepository.save(v);
    }
    @Transactional
    public Voucher createVoucher(VoucherRequest dto) {
        validateVoucher(dto, null);

        Voucher v = new Voucher();
        v.setCode("TEMP_" + System.currentTimeMillis());
        v.setName(ValidationSupport.trim(dto.getName()));
        v.setType(ValidationSupport.trim(dto.getType()));
        v.setValue(dto.getValue());
        v.setMaxDiscount(dto.getMaxDiscount());
        v.setMinOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue() : 0L);
        v.setQuantity(dto.getQuantity());
        v.setUsedCount(0);
        v.setIsPublic(dto.getIsPublic());
        v.setDescription(dto.getDescription());
        v.setStartDate(dto.getStartDate());
        v.setEndDate(dto.getEndDate());
        v.setLimitCustomerDays(dto.getLimitCustomerDays());
        v.setApplyBirthdayMonth(dto.getApplyBirthdayMonth());
        v.setMinCustomerSpent(dto.getMinCustomerSpent());
        v.setMaxDaysSinceLastOrder(dto.getMaxDaysSinceLastOrder());
        v.setIsFirstOrderOnly(dto.getIsFirstOrderOnly());

        applyVoucherStatus(v);
        Voucher saved = voucherRepository.save(v);
        String code = "VC" + String.format("%05d", saved.getId());
        if (voucherRepository.existsByCodeNormalizedAndIdNot(code, saved.getId())) {
            throw new ValidationException("code", "Mã voucher đã tồn tại.");
        }
        saved.setCode(code);
        return voucherRepository.save(saved);
    }
    public List<CustomerVoucherDTO> getAllCustomersForVoucher() {
        return customerRepository.findAllForVoucher();
    }
    // 1. Hàm lấy chi tiết Voucher
    public VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher với ID: " + id));
        return mapToResponse(voucher);
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherRequest dto) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher để cập nhật"));
        validateVoucher(dto, id);

        voucher.setName(ValidationSupport.trim(dto.getName()));
        voucher.setType(ValidationSupport.trim(dto.getType()));
        voucher.setValue(dto.getValue());
        voucher.setMaxDiscount(dto.getMaxDiscount());
        voucher.setMinOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue() : 0L);
        voucher.setQuantity(dto.getQuantity());
        voucher.setStartDate(dto.getStartDate());
        voucher.setEndDate(dto.getEndDate());
        voucher.setIsPublic(dto.getIsPublic());
        voucher.setDescription(dto.getDescription());
        voucher.setLimitCustomerDays(dto.getLimitCustomerDays());
        voucher.setApplyBirthdayMonth(dto.getApplyBirthdayMonth());
        voucher.setMinCustomerSpent(dto.getMinCustomerSpent());
        voucher.setMaxDaysSinceLastOrder(dto.getMaxDaysSinceLastOrder());
        voucher.setIsFirstOrderOnly(dto.getIsFirstOrderOnly());
        applyVoucherStatus(voucher);

        Voucher updatedVoucher = voucherRepository.save(voucher);
        return mapToResponse(updatedVoucher);
    }
    public List<CustomerVoucherDTO> getCustomersByVoucherId(Long voucherId) {
        return voucherCustomerRepository.findByVoucherId(voucherId)
                .stream()
                .map(vc -> {
                    Customer c = vc.getCustomer();
                    return CustomerVoucherDTO.builder()
                            .id(c.getId())
                            .ten(c.getTen())
                            .email(c.getEmail())
                            .loaiKhach(c.getLoaiKhach() != null ? c.getLoaiKhach() : "NORMAL")
                            .ngaySinh(c.getNgaySinh())
                            .build();
                })
                .toList();
    }
    // Thêm vào VoucherService.java
    @javax.transaction.Transactional
    public void updateStatus(Long id, String status) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        // Nếu muốn an toàn hơn, check thêm: nếu bật (ACTIVE) mà hết hạn thì báo lỗi
        if (status.equals("ACTIVE") && voucher.getEndDate().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Voucher đã quá hạn, không thể bật!");
        }

        voucher.setStatus(status);
        voucherRepository.save(voucher);
    }


    // 🔥 ĐÃ FIX LỖI: Dùng Điểm tích lũy để tính toán thay vì gọi thuộc tính không tồn tại
    // 🔥 ĐÃ CẬP NHẬT: Thêm logic "Món quà trở lại" (Tạo TK > 30 ngày nhưng chưa mua hàng)
    public List<Voucher> getAvailableVouchers(Long customerId) {
        // 1. Lấy danh sách voucher thô từ DB
        List<Voucher> rawVouchers = voucherRepository.findAvailableVouchersForOrder(LocalDateTime.now(), customerId);

        // 2. Trích xuất cho Khách vãng lai (không đăng nhập)
        if (customerId == null) {
            return rawVouchers.stream()
                    .filter(v -> v.getMinCustomerSpent() == null || v.getMinCustomerSpent() <= 0)
                    .filter(v -> v.getIsFirstOrderOnly() == null || !v.getIsFirstOrderOnly())
                    .filter(v -> v.getApplyBirthdayMonth() == null || !v.getApplyBirthdayMonth())
                    .filter(v -> v.getLimitCustomerDays() == null || v.getLimitCustomerDays() <= 0)
                    .filter(v -> v.getMaxDaysSinceLastOrder() == null || v.getMaxDaysSinceLastOrder() <= 0)
                    .toList();
        }

        // 3. Khách hàng có tài khoản
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return rawVouchers;

        // 4. Lọc chặn qua luồng Stream
        return rawVouchers.stream().filter(v -> {

            // 👉 Lọc Voucher VIP (Check tổng chi tiêu)
            if (v.getMinCustomerSpent() != null && v.getMinCustomerSpent() > 0) {
                long currentPoints = customer.getDiemTichLuy() != null ? customer.getDiemTichLuy() : 0L;
                long estimatedSpent = currentPoints * 10000L;
                if (estimatedSpent < v.getMinCustomerSpent()) {
                    return false;
                }
            }

            // 👉 Lọc Voucher Sinh nhật
            if (Boolean.TRUE.equals(v.getApplyBirthdayMonth())) {
                if (customer.getNgaySinh() == null ||
                        customer.getNgaySinh().getMonthValue() != LocalDateTime.now().getMonthValue()) {
                    return false;
                }
            }

            // 👉 Lọc Voucher Đơn đầu tiên
            if (Boolean.TRUE.equals(v.getIsFirstOrderOnly())) {
                long currentPoints = customer.getDiemTichLuy() != null ? customer.getDiemTichLuy() : 0L;
                if (currentPoints > 0) {
                    return false;
                }
            }

            // 👉 Lọc Voucher Khách hàng mới lập tài khoản (< 7 ngày)
            if (v.getLimitCustomerDays() != null && v.getLimitCustomerDays() > 0) {
                if (customer.getCreatedAt() != null) {
                    long daysSinceCreated = java.time.temporal.ChronoUnit.DAYS.between(customer.getCreatedAt(), LocalDateTime.now());
                    if (daysSinceCreated > v.getLimitCustomerDays()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            // 👉 Lọc Voucher Món quà trở lại (Đúng ý bạn: > 30 ngày tạo TK nhưng chưa mua)
            if (v.getMaxDaysSinceLastOrder() != null && v.getMaxDaysSinceLastOrder() > 0) {
                long currentPoints = customer.getDiemTichLuy() != null ? customer.getDiemTichLuy() : 0L;

                // Điều kiện 1: Khách ĐÃ MUA hàng (có điểm) -> KHÔNG cho hiện
                if (currentPoints > 0) {
                    return false;
                }

                // Điều kiện 2: Khách CHƯA MUA -> Check xem tài khoản tạo quá 30 ngày chưa
                if (customer.getCreatedAt() != null) {
                    long daysSinceCreated = java.time.temporal.ChronoUnit.DAYS.between(customer.getCreatedAt(), LocalDateTime.now());

                    // Nếu thời gian tạo TK đến nay <= 30 ngày (ví dụ mới tạo 20 ngày) -> KHÔNG cho hiện
                    if (daysSinceCreated <= v.getMaxDaysSinceLastOrder()) {
                        return false;
                    }
                } else {
                    return false; // An toàn: Tránh lỗi nếu tài khoản bị mất dữ liệu ngày tạo
                }
            }

            return true;
        }).toList();
    }

    private void validateVoucher(VoucherRequest dto, Long currentId) {
        String name = ValidationSupport.trim(dto.getName());
        if (name == null) throw new ValidationException("name", "Tên voucher không được để trống.");
        boolean dupName = currentId == null ? voucherRepository.existsByNameNormalized(name) : voucherRepository.existsByNameNormalizedAndIdNot(name, currentId);
        if (dupName) throw new ValidationException("name", "Tên voucher đã tồn tại.");
        if (dto.getStartDate() == null || dto.getEndDate() == null || !dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new ValidationException("startDate", "Ngày bắt đầu phải trước ngày kết thúc.");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new ValidationException("quantity", "Số lượng voucher phải lớn hơn 0.");
        }
        if (dto.getValue() == null || dto.getValue() <= 0) {
            throw new ValidationException("value", "Giá trị giảm không hợp lệ.");
        }
        if ("PERCENT".equalsIgnoreCase(dto.getType()) && dto.getValue() > 100) {
            throw new ValidationException("value", "Phần trăm giảm giá không được vượt quá 100.");
        }
    }

    private void applyVoucherStatus(Voucher v) {
        LocalDateTime now = LocalDateTime.now();
        if (v.getStartDate().isAfter(now)) v.setStatus("INACTIVE");
        else if (v.getEndDate().isBefore(now)) v.setStatus("EXPIRED");
        else v.setStatus("ACTIVE");
    }

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void autoCreateMonthlyFreeShipVoucher() {
        LocalDateTime now = LocalDateTime.now();
        String monthYear = now.format(DateTimeFormatter.ofPattern("MM/yyyy"));

        Voucher v = new Voucher();
        v.setName("Miễn Phí Vận Chuyển Tháng " + monthYear);
        v.setCode("FS" + now.format(DateTimeFormatter.ofPattern("MMyy")));
        v.setType("SHIPPING"); // Loại voucher là phí ship
        v.setValue(500000L); // Tối đa 500.000đ
        v.setMaxDiscount(500000L);
        v.setMinOrderValue(0L); // Đơn tối thiểu 0đ
        v.setQuantity(999999); // Số lượng cực lớn để ai cũng lấy được
        v.setUsedCount(0);
        v.setIsPublic(true); // Công khai cho tất cả mọi người
        v.setStatus("ACTIVE");

        // Thời gian áp dụng: Từ đầu tháng đến cuối tháng
        v.setStartDate(now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
        v.setEndDate(now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59));

        v.setDescription("Ưu đãi giảm tối đa 500k phí vận chuyển cho mọi khách hàng trong tháng " + monthYear);
        v.setDeleted(false);

        voucherRepository.save(v);
    }

}