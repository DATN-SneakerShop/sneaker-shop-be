package com.sneakershop.backend.repository.voucher;

import com.sneakershop.backend.entity.voucher.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    List<Voucher> findByStatus(String status);

    boolean existsByName(String name);

    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' " +
            "AND :now BETWEEN v.startDate AND v.endDate " +
            "AND v.deleted = false")
    List<Voucher> findActiveVouchersForReport(@Param("now") LocalDateTime now);

    /**
     * Lấy danh sách voucher khả dụng khi lên đơn hàng:
     * - KHÁCH LẺ (customerId = null hoặc 0): CHỈ lấy voucher thủ công (không check sinh nhật, thành viên).
     * - KHÁCH CÓ TÀI KHOẢN: Lấy voucher thủ công + voucher sinh nhật/thành viên (nếu đủ đk) + voucher riêng.
     */
    @Query(value = "SELECT v.* FROM voucher v " +
            "LEFT JOIN khach_hang c ON c.id = :customerId " +
            "WHERE v.trang_thai = 'ACTIVE' AND v.deleted = false " +
            "AND v.bat_dau <= :now AND v.ket_thuc >= :now " +
            "AND v.da_su_dung < v.so_luong " +
            "AND (" +
            "  /* 1. NẾU LÀ KHÁCH LẺ (ID rỗng hoặc 0): BẮT BUỘC bỏ qua các mẫu thiết lập nhanh */ " +
            "  ((:customerId IS NULL OR :customerId = 0) " +
            "    AND v.cong_khai = true " +
            "    AND v.limit_customer_days IS NULL " +     // Chặn Voucher Khách mới
            "    AND v.apply_birthday_month = false) " +   // Chặn Voucher Sinh nhật
            "  OR " +
            "  /* 2. NẾU CÓ KHÁCH HÀNG: Check đủ các điều kiện sinh nhật, ngày tạo, hoặc voucher gán riêng */ " +
            "  (:customerId > 0 AND c.id IS NOT NULL AND (" +
            "    (v.cong_khai = true AND " +
            "       (v.limit_customer_days IS NULL OR c.tao_luc >= :now - INTERVAL v.limit_customer_days DAY) AND " +
            "       (v.apply_birthday_month = false OR MONTH(c.ngay_sinh) = MONTH(:now))" +
            "    ) " +
            "    OR EXISTS (SELECT 1 FROM voucher_customer vc WHERE vc.voucher_id = v.id AND vc.customer_id = :customerId)" +
            "  ))" +
            ") " +
            "AND ((:customerId IS NULL OR :customerId = 0) OR NOT EXISTS (SELECT 1 FROM voucher_usage vu WHERE vu.voucher_id = v.id AND vu.customer_id = :customerId))",
            nativeQuery = true)
    List<Voucher> findAvailableVouchersForOrder(@Param("now") LocalDateTime now, @Param("customerId") Long customerId);
}