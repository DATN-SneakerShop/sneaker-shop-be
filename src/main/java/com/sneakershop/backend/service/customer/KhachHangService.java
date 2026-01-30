package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.KhachHang;
import com.sneakershop.backend.repository.customer.KhachHangRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KhachHangService {

    private final KhachHangRepository repository;

    public KhachHangService(KhachHangRepository repository) {
        this.repository = repository;
    }

    // 1️⃣ Hiển thị danh sách khách hàng
    public List<KhachHang> getAllActive() {
        return repository.findByStatus("ACTIVE");
    }

    // 2️⃣ Thêm khách hàng
    public KhachHang create(KhachHang kh) {
        kh.setStatus("ACTIVE");
        if (kh.getLoaiKhach() == null) {
            kh.setLoaiKhach("NORMAL"); // mặc định thường
        }
        return repository.save(kh);
    }

    // 3️⃣ Cập nhật / phân loại VIP - thường
    public KhachHang update(Long id, KhachHang data) {
        KhachHang kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        kh.setTen(data.getTen());
        kh.setEmail(data.getEmail());
        kh.setSoDienThoai(data.getSoDienThoai());
        kh.setNgaySinh(data.getNgaySinh());
        kh.setLoaiKhach(data.getLoaiKhach());

        return repository.save(kh);
    }

    // 4️⃣ Xoá mềm khách hàng
    public void delete(Long id) {
        KhachHang kh = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        kh.setStatus("INACTIVE");
        repository.save(kh);
    }
}
