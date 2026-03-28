package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.dto.customer.CustomerHistoryDTO;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.customer.CustomerPointHistory;
import com.sneakershop.backend.entity.customer.CustomerRankHistory;
import com.sneakershop.backend.repository.customer.CustomerPointHistoryRepository;
import com.sneakershop.backend.repository.customer.CustomerRankHistoryRepository;
import com.sneakershop.backend.repository.customer.CustomerRepository;
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
    private final CustomerRepository customerRepo;

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
    public List<Customer> filter(
            @RequestParam String loaiKhach,
            @RequestParam(required = false) Integer inactiveDays
    ) {
        return service.filter(loaiKhach, inactiveDays);
    }
    @GetMapping("/history/all")
    public Map<String, Object> getHistory() {

        List<CustomerRankHistory> rankHistory = rankRepo.findAll();
        List<CustomerPointHistory> pointHistory = pointRepo.findAll();

        // 👉 map thêm tên khách hàng
        rankHistory.forEach(r -> {
            Customer c = customerRepo.findById(r.getCustomerId()).orElse(null);
            if (c != null) {
                r.setCustomerName(c.getTen());
            }
        });

        pointHistory.forEach(p -> {
            Customer c = customerRepo.findById(p.getCustomerId()).orElse(null);
            if (c != null) {
                p.setCustomerName(c.getTen());
            }
        });

        Map<String, Object> res = new HashMap<>();
        res.put("rankHistory", rankHistory);
        res.put("pointHistory", pointHistory);

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


    @GetMapping("/vip-notification")
    public String vipNotification(@RequestParam String email){

        Customer customer = service.findByEmail(email);

        if(customer == null){
            return "";
        }

        // khách VIP
        if("VIP".equalsIgnoreCase(customer.getLoaiKhach()) || customer.getDiemTichLuy() >= 1000){
            return "⭐ Bạn là khách hàng VIP - được hưởng ưu đãi đặc biệt!";
        }

        // khách gần lên VIP
        if(customer.getDiemTichLuy() >= 900){
            return "⚠ Bạn chỉ còn " + (1000 - customer.getDiemTichLuy()) + " điểm nữa để lên VIP";
        }

        return "";
    }

    // Tìm khách theo tên, sđt, email
    @GetMapping("/search")
    public List<Customer> search(@RequestParam String keyword) {
        return service.search(keyword);
    }
}