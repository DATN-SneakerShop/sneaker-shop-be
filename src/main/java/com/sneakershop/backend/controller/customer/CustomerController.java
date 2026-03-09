package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.dto.customer.CustomerHistoryDTO;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.repository.customer.CustomerPointHistoryRepository;
import com.sneakershop.backend.repository.customer.CustomerRankHistoryRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService service;
    private final CustomerRankHistoryRepository rankRepo;
    private final CustomerPointHistoryRepository pointRepo;
    private final OrderRepository orderRepo;

    @GetMapping
    public List<Customer> getAll() { return service.getAllActive(); }

    @PostMapping
    public Customer create(@RequestBody Customer kh) { return service.create(kh); }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer kh) { return service.update(id, kh); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }

    @DeleteMapping("/all")
    public void clearAll() { service.deleteAllCustomers(); }

    @GetMapping("/filter")
    public List<Customer> filter(@RequestParam String loaiKhach) { return service.filterByLoai(loaiKhach); }

    @GetMapping("/history/all")
    public Map<String, Object> getHistory() {
        Map<String, Object> res = new HashMap<>();
        res.put("rankHistory", rankRepo.findAll());
        res.put("pointHistory", pointRepo.findAll());
        return res;
    }

    // Lịch sử giao dịch khách hàng
    @GetMapping("/history")
    public List<CustomerHistoryDTO> getHistorySpending(){
        return orderRepo.getCustomerHistory();
    }

    @DeleteMapping("/history/{id}")
    public void deleteHistory(@PathVariable Long id){
        orderRepo.deleteById(id);
    }
}