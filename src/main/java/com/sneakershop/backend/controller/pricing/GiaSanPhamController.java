package com.sneakershop.backend.controller.pricing;




import com.sneakershop.backend.AuditAction;
import com.sneakershop.backend.dto.pricing.BangGiaDTO;
import com.sneakershop.backend.dto.pricing.GiaRequest;
import com.sneakershop.backend.entity.pricing.GiaSanPham;
import com.sneakershop.backend.service.pricing.GiaSanPhamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@RestController
//@RequestMapping("/api/gia")
//@RequiredArgsConstructor
//public class GiaSanPhamController {
//
//    private final GiaSanPhamService giaSanPhamService;
//
//    // Danh sách giá
//    @GetMapping("/san-pham/{id}")
//    public List<GiaSanPham> layGiaTheoSanPham(@PathVariable Long id) {
//        return giaSanPhamService.layDanhSachGiaTheoSanPham(id);
//    }
//
//    // Giá hiện tại
//    @GetMapping("/hien-tai/{id}")
//    public BigDecimal layGiaHienTai(@PathVariable Long id) {
//        return giaSanPhamService.layGiaHienTai(id);
//    }
//
//
//    // Xóa
//    @DeleteMapping("/{id}")
//    public void xoaGia(@PathVariable Long id) {
//        giaSanPhamService.xoaGia(id);
//    }
//}
@RestController
@RequestMapping("/api/gia")
@RequiredArgsConstructor
public class GiaSanPhamController {

    private final GiaSanPhamService service;

    @GetMapping("/bang-gia")
    public List<BangGiaDTO> bangGia() {
        return service.layBangGia();
    }

    @GetMapping("/san-pham/{id}")
    public List<GiaSanPham> giaTheoSanPham(@PathVariable Long id) {
        return service.layGiaTheoSanPham(id);
    }

    @PostMapping("/san-pham/{id}")
    @AuditAction(
            module = "PRICING",
            action = "UPDATE_PRICE",
            entity = "GIA_SAN_PHAM"
    )
    public GiaSanPham suaGia(
            @PathVariable Long id,
            @RequestBody GiaRequest request
    ) {
        return service.suaGia(id, request);
    }

    @DeleteMapping("/{id}")
    @AuditAction(
            module = "PRICING",
            action = "DELETE_PRICE",
            entity = "GIA_SAN_PHAM"
    )
    public void xoaGia(@PathVariable Long id) {
        service.xoaGia(id);
    }
}


