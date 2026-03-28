package com.sneakershop.backend.service.voucher;

import com.sneakershop.backend.dto.voucher.CustomerVoucherDTO;
import com.sneakershop.backend.dto.voucher.VoucherRequest;
import com.sneakershop.backend.dto.voucher.VoucherResponse;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.voucher.Voucher;
import com.sneakershop.backend.entity.voucher.VoucherCustomer;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.voucher.VoucherCustomerRepository;
import com.sneakershop.backend.repository.voucher.VoucherRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final VoucherCustomerRepository voucherCustomerRepository;

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
        // 1. Kiểm tra tên tránh trùng (gây lỗi SQL Unique)
        if (voucherRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Tên voucher đã tồn tại!");
        }

        // 2. Kiểm tra ngày tháng (tránh NullPointerException)
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new RuntimeException("Ngày bắt đầu và kết thúc không được để trống");
        }

        Voucher v = new Voucher();
        v.setCode("TEMP_" + System.currentTimeMillis());
        v.setName(dto.getName());
        v.setType(dto.getType());

        // Chuyển BigDecimal từ DTO sang Long của Entity
        v.setValue(dto.getValue() != null ? dto.getValue().longValue() : 0L);
        v.setMaxDiscount(dto.getMaxDiscount() != null ? dto.getMaxDiscount().longValue() : null);
        v.setMinOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue().longValue() : 0L);

        v.setQuantity(dto.getQuantity());
        v.setUsedCount(0);
        v.setIsPublic(dto.getIsPublic());
        v.setDescription(dto.getDescription());
        v.setStartDate(dto.getStartDate());
        v.setEndDate(dto.getEndDate());

        // 3. Tự động tính trạng thái
        LocalDateTime now = LocalDateTime.now();
        if (dto.getStartDate().isAfter(now)) {
            v.setStatus("INACTIVE");
        } else if (dto.getEndDate().isBefore(now)) {
            v.setStatus("EXPIRED");
        } else {
            v.setStatus("ACTIVE");
        }

        Voucher saved = voucherRepository.save(v);
        saved.setCode("VC" + String.format("%05d", saved.getId()));
        return voucherRepository.save(saved);
    }
    public List<CustomerVoucherDTO> getAllCustomersForVoucher() {
        return customerRepository.findAllForVoucher();
    }
    // 1. Hàm lấy chi tiết Voucher
    public VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher với ID: " + id));
        return mapToResponse(voucher); // Giả sử bạn đã có hàm mapToResponse hoặc tự tạo DTO mới
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherRequest dto) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher để cập nhật"));

        voucher.setName(dto.getName());
        voucher.setType(dto.getType());

        // 🔥 QUAN TRỌNG: Ép kiểu BigDecimal sang Long để tránh lỗi 500
        voucher.setValue(dto.getValue() != null ? dto.getValue().longValue() : 0L);
        voucher.setMaxDiscount(dto.getMaxDiscount() != null ? dto.getMaxDiscount().longValue() : null);
        voucher.setMinOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue().longValue() : 0L);

        voucher.setQuantity(dto.getQuantity());
        voucher.setStartDate(dto.getStartDate());
        voucher.setEndDate(dto.getEndDate());
        voucher.setIsPublic(dto.getIsPublic());
        voucher.setDescription(dto.getDescription());

        // Tự động cập nhật lại trạng thái theo thời gian mới sửa
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate().isAfter(now)) voucher.setStatus("INACTIVE");
        else if (voucher.getEndDate().isBefore(now)) voucher.setStatus("EXPIRED");
        else voucher.setStatus("ACTIVE");

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
                            // 🔥 SỬA TẠI ĐÂY: Vì c.getLoaiKhach() đã là String nên lấy luôn
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
    public List<Voucher> getAvailableVouchers(Long customerId) {
        return voucherRepository.findAvailableVouchers(LocalDateTime.now(), customerId);
    }
}