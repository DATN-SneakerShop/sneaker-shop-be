package com.sneakershop.backend.repository.pricing;


import com.sneakershop.backend.dto.pricing.BangGiaDTO;
import com.sneakershop.backend.entity.pricing.GiaSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GiaSanPhamRepository extends JpaRepository<GiaSanPham, Long> {
//
//    // Danh sách giá theo sản phẩm
//    @Query("""
//        SELECT g FROM GiaSanPham g
//        WHERE g.sanPham.id = :sanPhamId
//        ORDER BY g.ngayBatDau DESC
//    """)
//    List<GiaSanPham> findBySanPhamId(@Param("sanPhamId") Long sanPhamId);
//
//    // Giá hiện tại
//    @Query("""
//        SELECT g FROM GiaSanPham g
//        WHERE g.sanPham.id = :sanPhamId
//          AND g.ngayBatDau <= CURRENT_TIMESTAMP
//          AND g.ngayKetThuc IS NULL
//    """)
//    Optional<GiaSanPham> findGiaHienTai(@Param("sanPhamId") Long sanPhamId);
@Query("""
    SELECT new com.sneakershop.backend.dto.pricing.BangGiaDTO(
        sp.id,
        sp.ten,
        g.gia
    )
    FROM GiaSanPham g
    JOIN g.sanPham sp
    WHERE g.ngayKetThuc IS NULL
""")
List<BangGiaDTO> layBangGia();


    List<GiaSanPham> findBySanPhamId(Long sanPhamId);

    Optional<GiaSanPham> findBySanPhamIdAndNgayKetThucIsNull(Long sanPhamId);

}
