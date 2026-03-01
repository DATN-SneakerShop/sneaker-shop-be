package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.repository.customer.CustomerPointHistoryRepository;
import com.sneakershop.backend.repository.customer.CustomerRankHistoryRepository;
import com.sneakershop.backend.service.customer.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/khach-hang")
public class CustomerController {

    private final CustomerService service;
    private final CustomerRankHistoryRepository customerRankHistoryRepository;
    private final CustomerPointHistoryRepository customerPointHistoryRepository;

    public CustomerController(CustomerService service, CustomerRankHistoryRepository customerRankHistoryRepository, CustomerPointHistoryRepository customerPointHistoryRepository) {
        this.service = service;
        this.customerRankHistoryRepository = customerRankHistoryRepository;
        this.customerPointHistoryRepository = customerPointHistoryRepository;
    }

    // 1️⃣ Danh sách khách hàng
    @GetMapping
    public List<Customer> getAll() {
        return service.getAllActive();
    }

    // 2️⃣ Tạo khách hàng
    @PostMapping
    public Customer create(@RequestBody Customer kh) {
        return service.create(kh);
    }

    // 3️⃣ Sửa + phân loại
    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id,
                           @RequestBody Customer kh) {
        return service.update(id, kh);
    }

    // 4️⃣ Xoá khách hàng
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/filter")
    public List<Customer> filterByLoai(@RequestParam String loaiKhach) {
        return service.filterByLoai(loaiKhach);
    }

    // Lich su VIP
    @GetMapping("/history/all")
    public Map<String, Object> getAllHistory() {

        Map<String, Object> result = new HashMap<>();

        result.put("rankHistory",
                customerRankHistoryRepository.findAll());

        result.put("pointHistory",
                customerPointHistoryRepository.findAll());

        return result;
    }
}
