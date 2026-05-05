package com.sneakershop.backend.service.login;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sneakershop.backend.audit.SystemAuditLogService;
import com.sneakershop.backend.dto.login.CurrentAccountResponse;
import com.sneakershop.backend.dto.login.CurrentAddressResponse;
import com.sneakershop.backend.dto.login.CurrentCustomerResponse;
import com.sneakershop.backend.dto.login.UpdateCurrentCustomerRequest;
import com.sneakershop.backend.dto.login.UpsertCurrentAddressRequest;
import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.customer.Address;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.Role;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.repository.customer.AddressRepository;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.login.RoleRepository;
import com.sneakershop.backend.repository.login.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final SystemAuditLogService systemAuditLogService;

    @Transactional
    public User loginWithGoogle(String idTokenString, String ip) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList("1090844427851-g1hfpluc79irjnftmvn620m8g261rfct.apps.googleusercontent.com"))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Token Google không hợp lệ hoặc đã hết hạn!");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User();
            user.setUsername(email);
            user.setEmail(email);
            user.setFullName(name);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setLoaiDangNhap("GOOGLE");
            user.setStatus("ACTIVE");

            Role customerRole = roleRepository.findByCode("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Role CUSTOMER chưa có trong hệ thống!"));
            user.setRoles(Set.of(customerRole));

            userRepository.save(user);

            systemAuditLogService.logManual(email, ip, "AUTH", "REGISTER_GOOGLE", "User",
                    "Đăng ký mới qua Google", "SUCCESS", null, "INFO");
        }

        ensureCustomerProfile(user);

        systemAuditLogService.logManual(email, ip, "AUTH", "LOGIN_GOOGLE", "User",
                "Đăng nhập bằng Google", "SUCCESS", null, "INFO");
        return user;
    }

    @Transactional
    public void registerLocal(UserRequest request, String ip) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã được đăng ký!");
        }

        User user = new User();
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setLoaiDangNhap("LOCAL");
        user.setStatus("ACTIVE");

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER chưa có trong hệ thống!"));
        user.setRoles(Set.of(customerRole));

        userRepository.save(user);

        ensureCustomerProfile(user);

        systemAuditLogService.logManual(
                request.getEmail(), ip, "AUTH", "REGISTER", "User",
                "Đăng ký mới qua Email: " + user.getEmail(), "SUCCESS", null, "INFO"
        );
    }

    @Transactional(readOnly = true)
    public CurrentAccountResponse getCurrentAccount(String principalName) {
        User user = resolveCurrentUser(principalName);

        CurrentAccountResponse response = new CurrentAccountResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRoles(
                user.getRoles().stream()
                        .map(Role::getCode)
                        .collect(Collectors.toList())
        );

        customerRepository.findByUserId(user.getId())
                .ifPresent(customer -> response.setCustomerInfo(mapCustomer(customer)));

        return response;
    }

    @Transactional(readOnly = true)
    public List<CurrentAddressResponse> getCurrentAddresses(String principalName) {
        User user = resolveCurrentUser(principalName);
        Customer customer = getOrCreateCustomerForUser(user);

        return addressRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapAddress)
                .collect(Collectors.toList());
    }

    @Transactional
    public CurrentCustomerResponse updateCurrentCustomer(String principalName, UpdateCurrentCustomerRequest request) {
        User user = resolveCurrentUser(principalName);
        Customer customer = getOrCreateCustomerForUser(user);

        String fullName = safeTrim(request.getFullName());
        String phone = safeTrim(request.getPhone());

        if (fullName == null || fullName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống");
        }
        if (!fullName.matches("^[a-zA-ZÀ-ỹ\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không hợp lệ");
        }

        if (phone != null && !phone.isEmpty()) {
            if (!phone.matches("0\\d{9}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SĐT không hợp lệ");
            }
            if (customerRepository.existsByPhoneAndIdNot(phone, customer.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại");
            }
        } else {
            phone = null;
        }

        LocalDate ngaySinh = request.getNgaySinh();
        if (ngaySinh != null && ngaySinh.plusYears(16).isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải đủ 16 tuổi");
        }

        user.setFullName(fullName);
        userRepository.save(user);

        customer.setTen(fullName);
        customer.setPhone(phone);
        customer.setNgaySinh(ngaySinh);
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            customer.setEmail(user.getEmail());
        }

        customerRepository.save(customer);
        return mapCustomer(customer);
    }

    @Transactional
    public CurrentAddressResponse createCurrentAddress(String principalName, UpsertCurrentAddressRequest request) {
        User user = resolveCurrentUser(principalName);
        Customer customer = getOrCreateCustomerForUser(user);

        Address address = new Address();
        address.setCustomer(customer);
        applyAddressRequest(address, request, customer.getId());

        addressRepository.save(address);
        return mapAddress(address);
    }

    @Transactional
    public CurrentAddressResponse updateCurrentAddress(String principalName, Long addressId, UpsertCurrentAddressRequest request) {
        User user = resolveCurrentUser(principalName);
        Customer customer = getOrCreateCustomerForUser(user);

        Address address = addressRepository.findByIdAndCustomerId(addressId, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));

        applyAddressRequest(address, request, customer.getId());

        addressRepository.save(address);
        return mapAddress(address);
    }

    @Transactional
    public void deleteCurrentAddress(String principalName, Long addressId) {
        User user = resolveCurrentUser(principalName);
        Customer customer = getOrCreateCustomerForUser(user);

        Address address = addressRepository.findByIdAndCustomerId(addressId, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));

        addressRepository.delete(address);
    }

    @Transactional
    public void generateOtpForPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email này!"));

        String otp = String.format("%06d", new Random().nextInt(1000000));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        sendEmail(email, "Mã xác thực OTP SneakerShop", "Mã của bạn là: " + otp + " (Hiệu lực trong 5 phút)");

        systemAuditLogService.logManual(
                email, "SYSTEM", "AUTH", "REQUEST_OTP", "User",
                "Yêu cầu mã OTP khôi phục mật khẩu", "SUCCESS", null, "INFO"
        );
    }

    @Transactional
    public void verifyAndResetPassword(String email, String otp, String newPassword, String ip) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại!"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("Mã OTP không chính xác!");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn, vui lòng lấy mã mới!");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        userRepository.save(user);

        systemAuditLogService.logManual(
                email, ip, "AUTH", "RESET_PASSWORD", "User",
                "Đổi mật khẩu thành công qua OTP", "SUCCESS", null, "INFO"
        );
    }

    private User resolveCurrentUser(String principalName) {
        if (principalName == null || principalName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }

        return userRepository.findByUsername(principalName)
                .or(() -> userRepository.findByEmail(principalName))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản hiện tại"
                ));
    }

    private Customer getOrCreateCustomerForUser(User user) {
        return customerRepository.findByUserId(user.getId())
                .orElseGet(() -> ensureCustomerProfile(user));
    }

    private Customer ensureCustomerProfile(User user) {
        return customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setTen(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail());
            customer.setEmail(user.getEmail());
            customer.setDiemTichLuy(0);
            customer.setLoaiKhach("BRONZE");
            customer.setStatus("ACTIVE");
            customer.setUser(user);
            return customerRepository.save(customer);
        });
    }

    private void applyAddressRequest(Address address, UpsertCurrentAddressRequest request, Long customerId) {
        String recipientName = safeTrim(request.getRecipientName());
        String phone = safeTrim(request.getPhone());
        String province = safeTrim(request.getProvince());
        String district = safeTrim(request.getDistrict());
        String ward = safeTrim(request.getWard());
        String detailAddress = safeTrim(request.getDetailAddress());
        Integer isDefault = request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0;

        if (recipientName == null || recipientName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên người nhận không được để trống");
        }
        if (phone == null || !phone.matches("0\\d{9}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SĐT không hợp lệ");
        }
        if (province == null || province.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỉnh/thành phố không được để trống");
        }
        if (district == null || district.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quận/huyện không được để trống");
        }
        if (ward == null || ward.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phường/xã không được để trống");
        }
        if (detailAddress == null || detailAddress.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ chi tiết không được để trống");
        }

        address.setLabel(safeTrim(request.getLabel()));
        address.setRecipientName(recipientName);
        address.setPhone(phone);
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        address.setDetailAddress(detailAddress);
        address.setIsDefault(isDefault);

        if (isDefault == 1) {
            List<Address> oldAddresses = addressRepository.findByCustomerId(customerId);
            for (Address old : oldAddresses) {
                if (address.getId() == null || !old.getId().equals(address.getId())) {
                    old.setIsDefault(0);
                }
            }
            addressRepository.saveAll(oldAddresses);
        }
    }

    private CurrentCustomerResponse mapCustomer(Customer customer) {
        CurrentCustomerResponse dto = new CurrentCustomerResponse();
        dto.setId(customer.getId());
        dto.setTen(customer.getTen());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setNgaySinh(customer.getNgaySinh());
        dto.setDiemTichLuy(customer.getDiemTichLuy());
        dto.setLoaiKhach(customer.getLoaiKhach());
        dto.setStatus(customer.getStatus());
        dto.setGhiChu(customer.getGhiChu());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        return dto;
    }

    private CurrentAddressResponse mapAddress(Address address) {
        CurrentAddressResponse dto = new CurrentAddressResponse();
        dto.setId(address.getId());
        dto.setLabel(address.getLabel());
        dto.setRecipientName(address.getRecipientName());
        dto.setPhone(address.getPhone());
        dto.setProvince(address.getProvince());
        dto.setDistrict(address.getDistrict());
        dto.setWard(address.getWard());
        dto.setDetailAddress(address.getDetailAddress());
        dto.setIsDefault(address.getIsDefault());
        return dto;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}