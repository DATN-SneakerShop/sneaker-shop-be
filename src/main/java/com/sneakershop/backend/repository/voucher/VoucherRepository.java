package com.sneakershop.backend.repository.voucher;

import com.sneakershop.backend.entity.voucher.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findByStatus(String status);

    boolean existsByName(String name);

    /**
     * Lấy danh sách voucher khả dụng cho khách hàng:
     * 1. Phải là ACTIVE và trong thời hạn sử dụng.
     * 2. Chưa hết số lượng tổng (usedCount < quantity).
     * 3. Voucher chưa bị xóa (deleted = false).
     * 4. Là voucher công khai HOẶC được gán riêng cho khách này.
     * 5. QUAN TRỌNG: Khách hàng này CHƯA từng sử dụng voucher này (không có trong VoucherUsage).
     */
    @Query(value = "SELECT v.* FROM voucher v " +
            "JOIN customer c ON c.id = :customerId " +
            "WHERE v.trang_thai = 'ACTIVE' AND v.deleted = false " +
            "AND v.bat_dau <= :now AND v.ket_thuc >= :now " +
            "AND v.da_su_dung < v.so_luong " +
            "AND (" +
            "  (v.cong_khai = true AND (" +
            "     (v.limit_customer_days IS NULL OR c.tao_luc >= :now - INTERVAL v.limit_customer_days DAY) AND " + // Khách mới
            "     (v.apply_birthday_month = false OR MONTH(c.ngay_sinh) = MONTH(:now)) AND " + // Sinh nhật
            "     (v.min_customer_spent IS NULL OR c.total_spent >= v.min_customer_spent) AND " + // VIP
            "     (v.max_days_since_last_order IS NULL OR (c.last_order_date IS NOT NULL AND c.last_order_date <= :now - INTERVAL v.max_days_since_last_order DAY)) AND " + // Khách cũ
            "     (v.is_first_order_only = false OR c.total_orders = 0)" + // Đơn đầu
            "  )) " +
            "  OR EXISTS (SELECT 1 FROM voucher_customer vc WHERE vc.voucher_id = v.id AND vc.customer_id = :customerId)" +
            ") " +
            "AND NOT EXISTS (SELECT 1 FROM voucher_usage vu WHERE vu.voucher_id = v.id AND vu.customer_id = :customerId)",
            nativeQuery = true)
    List<Voucher> findAvailableVouchers(@Param("now") LocalDateTime now, @Param("customerId") Long customerId);

    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' " +
            "AND :now BETWEEN v.startDate AND v.endDate " +
            "AND v.deleted = false")
    List<Voucher> findActiveVouchersForReport(@Param("now") LocalDateTime now);
}