package com.sneakershop.backend.service.pricing;



import com.sneakershop.backend.dto.pricing.BangGiaDTO;
import com.sneakershop.backend.dto.pricing.GiaRequest;
import com.sneakershop.backend.entity.pricing.GiaSanPham;
import com.sneakershop.backend.entity.pricing.SanPham;
import com.sneakershop.backend.entity.pricing.TienTe;
import com.sneakershop.backend.repository.pricing.GiaSanPhamRepository;
import com.sneakershop.backend.repository.pricing.SanPhamRepository;
import com.sneakershop.backend.repository.pricing.TienTeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GiaSanPhamService {
//
//    private final GiaSanPhamRepository giaSanPhamRepository;
//    private final TienTeService tienTeService;
//    private final SanPhamRepository sanPhamRepository;
//
//
//    public List<GiaSanPham> layDanhSachGiaTheoSanPham(Long sanPhamId) {
//        return giaSanPhamRepository.findBySanPhamId(sanPhamId);
//    }
//
//    public BigDecimal layGiaHienTai(Long sanPhamId) {
//        return giaSanPhamRepository.findGiaHienTai(sanPhamId)
//                .map(GiaSanPham::getGia)
//                .orElseThrow(() -> new RuntimeException("Sản phẩm chưa có giá"));
//
//    }
//
//
//    @Transactional
//    public GiaSanPham taoGiaMoi(Long sanPhamId, BigDecimal gia) {
//
//        SanPham sanPham = sanPhamRepository.findById(sanPhamId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
//
//        // Kết thúc giá hiện tại
//        giaSanPhamRepository.findGiaHienTai(sanPhamId)
//                .ifPresent(giaCu -> giaCu.setNgayKetThuc(LocalDateTime.now()));
//
//        GiaSanPham giaMoi = new GiaSanPham();
//        giaMoi.setSanPham(sanPham);
//        giaMoi.setTienTe(tienTeService.layTienTeMacDinh());
//        giaMoi.setGia(gia);
//        giaMoi.setNgayBatDau(LocalDateTime.now());
//        giaMoi.setNgayKetThuc(null);
//
//        return giaSanPhamRepository.save(giaMoi);
//    }
//
//
//
//
//    public void xoaGia(Long giaId) {
//        GiaSanPham gia = giaSanPhamRepository.findById(giaId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá"));
//
//        if (gia.getNgayKetThuc() == null) {
//            throw new RuntimeException("Không được xóa giá hiện tại");
//        }
//
//        giaSanPhamRepository.delete(gia);
//    }


    private final GiaSanPhamRepository giaRepo;
    private final SanPhamRepository sanPhamRepo;
    private final TienTeRepository tienTeRepo;

    // 1. Bảng giá
    public List<BangGiaDTO> layBangGia() {
        return giaRepo.layBangGia();
    }

    // 2. Danh sách giá theo sản phẩm
    public List<GiaSanPham> layGiaTheoSanPham(Long sanPhamId) {
        return giaRepo.findBySanPhamId(sanPhamId);
    }

    // 3. Sửa giá (tạo giá mới)
    public GiaSanPham suaGia(Long sanPhamId, GiaRequest request) {

        SanPham sp = sanPhamRepo.findById(sanPhamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // kết thúc giá cũ
        giaRepo.findBySanPhamIdAndNgayKetThucIsNull(sanPhamId)
                .ifPresent(g -> {
                    g.setNgayKetThuc(LocalDateTime.now());
                    g.setLaMacDinh(false);
                    giaRepo.save(g);
                });

        // tiền tệ
        TienTe tienTe = request.getTienTe() == null
                ? tienTeRepo.findByLaMacDinhTrue().orElseThrow()
                : tienTeRepo.findById(request.getTienTe()).orElseThrow();

        GiaSanPham giaMoi = new GiaSanPham();
        giaMoi.setSanPham(sp);
        giaMoi.setGia(request.getGia());
        giaMoi.setTienTe(tienTe);
        giaMoi.setNgayBatDau(LocalDateTime.now());
        giaMoi.setNgayKetThuc(null);
        giaMoi.setLaMacDinh(true);

        return giaRepo.save(giaMoi);
    }

    // 4. Xóa giá
    public void xoaGia(Long id) {
        GiaSanPham gia = giaRepo.findById(id).orElseThrow();
        if (gia.getNgayKetThuc() == null) {
            throw new RuntimeException("Không được xóa giá hiện tại");
        }
        giaRepo.delete(gia);
    }
}


