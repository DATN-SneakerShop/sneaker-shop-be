package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.dto.customer.CustomerHistoryDTO;
import com.sneakershop.backend.dto.customer.CustomerDashboardDTO;
import com.sneakershop.backend.dto.customer.CustomerSpendingDTO;
import com.sneakershop.backend.dto.customer.CustomerTransactionDTO;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.customer.CustomerPointHistory;
import com.sneakershop.backend.entity.customer.CustomerRankHistory;
import com.sneakershop.backend.repository.customer.CustomerPointHistoryRepository;
import com.sneakershop.backend.repository.customer.CustomerRankHistoryRepository;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.service.customer.CustomerService;
import com.sneakershop.backend.service.customer.CustomerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CustomerAnalyticsService analyticsService;

    @GetMapping
    public List<Customer> getAll() { return service.getAllActive(); }

    @PostMapping
    public Customer create(@RequestBody Customer kh) { return service.create(kh); }

    // Gọi thêm UserRepository và CustomerRankRepository
    @org.springframework.beans.factory.annotation.Autowired
    private com.sneakershop.backend.repository.login.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.sneakershop.backend.repository.customer.CustomerRankRepository customerRankRepository;

    @PutMapping("/{id}")
    public org.springframework.http.ResponseEntity<?> update(@PathVariable Long id, @RequestBody Customer kh) {
        // 1. Tìm khách hàng cũ đang có trong Database
        Customer existingCustomer = customerRepo.findById(id).orElse(null);

        if (existingCustomer == null) {
            return org.springframework.http.ResponseEntity.badRequest().body("Lỗi: Không tìm thấy khách hàng!");
        }

        // 2. Cập nhật bảng Customer
        if (kh.getTen() != null && !kh.getTen().isEmpty()) {
            existingCustomer.setTen(kh.getTen());

            // Đồng bộ họ tên sang cả bảng User để Web nhận diện được
            if (existingCustomer.getUser() != null) {
                existingCustomer.getUser().setFullName(kh.getTen());
                userRepository.save(existingCustomer.getUser());
            }
        }

        existingCustomer.setPhone(kh.getPhone());
        existingCustomer.setNgaySinh(kh.getNgaySinh());

        // ======================================================================
        // 3. LOGIC TỰ ĐỘNG: XỬ LÝ ĐIỂM TÍCH LŨY VÀ XÉT HẠNG
        // ======================================================================
        if (kh.getDiemTichLuy() != null && !kh.getDiemTichLuy().equals(existingCustomer.getDiemTichLuy())) {
            Integer oldPoints = existingCustomer.getDiemTichLuy() != null ? existingCustomer.getDiemTichLuy() : 0;
            Integer newPoints = kh.getDiemTichLuy();

            // A. Lưu lịch sử thay đổi Điểm
            CustomerPointHistory pointHistory = new CustomerPointHistory();
            pointHistory.setCustomerId(existingCustomer.getId());
            pointHistory.setOldPoint(oldPoints);
            pointHistory.setNewPoint(newPoints);
            pointHistory.setReason("Admin cập nhật điểm thủ công");
            pointRepo.save(pointHistory);

            // B. Cập nhật điểm mới cho khách
            existingCustomer.setDiemTichLuy(newPoints);

            // C. Xét Hạng (So sánh với bảng customer_rank_config)
            java.util.List<com.sneakershop.backend.entity.customer.CustomerRank> ranks =
                    customerRankRepository.findAllByOrderByMinPointsDesc();

            String newRankName = "NORMAL"; // Mặc định nếu không đạt mốc nào

            for (com.sneakershop.backend.entity.customer.CustomerRank r : ranks) {
                if (newPoints >= r.getMinPoints()) {
                    newRankName = r.getName(); // Lấy tên hạng cao nhất mà điểm vừa vượt qua
                    break;
                }
            }

            // D. Nếu hạng mới khác hạng cũ -> Lưu lịch sử hạng và cập nhật
            String oldRank = existingCustomer.getLoaiKhach();
            if (oldRank == null || oldRank.isEmpty()) oldRank = "NORMAL";

            if (!newRankName.equals(oldRank)) {
                CustomerRankHistory rankHistory = new CustomerRankHistory();
                rankHistory.setCustomerId(existingCustomer.getId());
                rankHistory.setOldRank(oldRank);
                rankHistory.setNewRank(newRankName);
                rankHistory.setReason("Điểm đạt mốc xét hạng mới: " + newPoints);
                rankRepo.save(rankHistory);

                existingCustomer.setLoaiKhach(newRankName);
            }
        }
        // ======================================================================

        // 4. Lưu lại bản ghi đã cập nhật
        return org.springframework.http.ResponseEntity.ok(customerRepo.save(existingCustomer));
    }

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

    // Thống kê chi tiêu khách hàng
    @GetMapping("/spending")
    public List<CustomerSpendingDTO> getCustomerSpending(){
        return analyticsService.spending();
    }

    @GetMapping("/top-spending")
    public List<CustomerSpendingDTO> getTopSpending(@RequestParam(name = "limit", required = false, defaultValue = "3") Integer limit){
        return analyticsService.topCustomers(limit);
    }

    @GetMapping("/vip")
    public List<CustomerSpendingDTO> getVipCustomers(
            @RequestParam(name = "minPoint", required = false) Integer minPoint,
            @RequestParam(name = "maxPoint", required = false) Integer maxPoint
    ){
        return analyticsService.vipCustomers(minPoint, maxPoint);
    }

    @GetMapping("/dashboard")
    public CustomerDashboardDTO getCustomerDashboard(){
        return analyticsService.dashboard();
    }

    // Lịch sử giao dịch khách hàng
    @GetMapping("/history")
    public List<CustomerTransactionDTO> getHistorySpending(){
        return analyticsService.transactions();
    }

    @DeleteMapping("/history/{id}")
    public org.springframework.http.ResponseEntity<?> deleteHistory(@PathVariable Long id){
        return org.springframework.http.ResponseEntity.badRequest().body("Không được xóa đơn hàng từ màn lịch sử giao dịch. Vui lòng xử lý trong quản lý đơn hàng.");
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

    @org.springframework.beans.factory.annotation.Autowired
    private com.sneakershop.backend.repository.customer.AddressRepository addressRepository;

    @GetMapping("/{targetId}/dia-chi")
    public org.springframework.http.ResponseEntity<?> getAddressesByCustomer(@PathVariable Long targetId) {
        Customer customer = customerRepo.findByUser_Id(targetId)
                .orElseGet(() -> customerRepo.findById(targetId).orElse(null));

        if (customer == null) {
            return org.springframework.http.ResponseEntity.ok(java.util.Collections.emptyList());
        }

        // Trả về DTO giống hệt bên Web để đảm bảo đồng bộ 100%
        java.util.List<com.sneakershop.backend.entity.customer.Address> list = addressRepository.findByCustomerId(customer.getId());
        java.util.List<com.sneakershop.backend.dto.login.CurrentAddressResponse> response = new java.util.ArrayList<>();

        for (com.sneakershop.backend.entity.customer.Address addr : list) {
            com.sneakershop.backend.dto.login.CurrentAddressResponse dto = new com.sneakershop.backend.dto.login.CurrentAddressResponse();
            dto.setId(addr.getId());
            dto.setLabel(addr.getLabel());
            dto.setRecipientName(addr.getRecipientName());
            dto.setPhone(addr.getPhone());
            dto.setProvince(addr.getProvince());
            dto.setDistrict(addr.getDistrict());
            dto.setWard(addr.getWard());
            dto.setDetailAddress(addr.getDetailAddress());
            dto.setIsDefault(addr.getIsDefault());
            response.add(dto);
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping("/{targetId}/dia-chi")
    public org.springframework.http.ResponseEntity<?> saveAddressForCustomer(
            @PathVariable Long targetId,
            @RequestBody com.sneakershop.backend.dto.login.UpsertCurrentAddressRequest request) { // Dùng DTO giống web

        Customer customer = customerRepo.findByUser_Id(targetId)
                .orElseGet(() -> customerRepo.findById(targetId).orElse(null));

        if (customer == null) {
            return org.springframework.http.ResponseEntity.badRequest().body("Lỗi: Không tìm thấy hồ sơ Khách hàng!");
        }

        com.sneakershop.backend.entity.customer.Address address = new com.sneakershop.backend.entity.customer.Address();
        address.setCustomer(customer);
        address.setLabel(request.getLabel());
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);

        if (address.getIsDefault() == 1) {
            java.util.List<com.sneakershop.backend.entity.customer.Address> oldList =
                    addressRepository.findByCustomerId(customer.getId());
            for (com.sneakershop.backend.entity.customer.Address old : oldList) {
                old.setIsDefault(0);
                addressRepository.save(old);
            }
        }

        addressRepository.save(address);
        return org.springframework.http.ResponseEntity.ok(address);
    }

    @DeleteMapping("/dia-chi/{addressId}")
    public org.springframework.http.ResponseEntity<?> deleteAddress(@PathVariable Long addressId) {
        addressRepository.deleteById(addressId);
        return org.springframework.http.ResponseEntity.ok("{\"message\": \"Xóa địa chỉ thành công!\"}");
    }
}