package com.sneakershop.backend.service.order;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductImage;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.voucher.Voucher;
import com.sneakershop.backend.repository.login.UserRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.repository.voucher.VoucherRepository;
import com.sneakershop.backend.service.customer.CustomerService;
import com.sneakershop.backend.service.notification.TelegramNotificationService;
import com.sneakershop.backend.service.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductVariantRepository productVariantRepository;
    private final EntityManager em;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerService customerService;
    private final VoucherService voucherService;
    private final OrderInventoryService orderInventoryService;
    private final TelegramNotificationService telegramNotificationService;

    private Order getOrderOr404(Long id) {
        return orderRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private ProductVariant getVariantOr404(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found: " + id));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private int nzInt(Integer v) {
        return v == null ? 0 : v;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveProductImageSnapshot(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        if (!isBlank(variant.getImageUrl())) {
            return variant.getImageUrl().trim();
        }

        Product product = variant.getProduct();
        if (product == null) {
            return null;
        }
        if (!isBlank(product.getThumbnail())) {
            return product.getThumbnail().trim();
        }

        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            return null;
        }

        return images.stream()
                .filter(Objects::nonNull)
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .filter(url -> !isBlank(url))
                .map(String::trim)
                .findFirst()
                .orElseGet(() -> images.stream()
                        .filter(Objects::nonNull)
                        .map(ProductImage::getImageUrl)
                        .filter(url -> !isBlank(url))
                        .map(String::trim)
                        .findFirst()
                        .orElse(null));
    }

    private void snapshotOrderItem(OrderItem item, ProductVariant variant) {
        if (item == null || variant == null) {
            return;
        }

        Product product = variant.getProduct();
        item.setProductIdSnapshot(product != null ? product.getId() : null);
        item.setVariantIdSnapshot(variant.getId());
        item.setSkuSnapshot(!isBlank(item.getSkuSnapshot()) ? item.getSkuSnapshot() : variant.getSku());
        item.setProductNameSnapshot(!isBlank(item.getProductNameSnapshot()) ? item.getProductNameSnapshot() : buildVariantName(variant));
        item.setColorSnapshot(variant.getColor() != null ? variant.getColor().getName() : null);
        item.setSizeSnapshot(variant.getSize() != null ? variant.getSize().getName() : null);
        item.setMaterialSnapshot(product != null && product.getMaterial() != null ? product.getMaterial().getName() : null);
        item.setSoleSnapshot(product != null && product.getSole() != null ? product.getSole().getName() : null);
        item.setImageUrlSnapshot(resolveProductImageSnapshot(variant));
    }

    private void validateAdminOnlineShippingInfo(CreateOrderRequest req) {
        if (req.getChannel() != SalesChannel.ONLINE) {
            return;
        }
        if (com.sneakershop.backend.entity.order.enums.PaymentMethod.CASH.equals(req.getPaymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn online chỉ hỗ trợ COD hoặc chuyển khoản.");
        }
        if (isBlank(req.getOrdererName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên người đặt không được để trống");
        }
        if (isBlank(req.getOrdererPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại người đặt không được để trống");
        }
        if (!isBlank(req.getOrdererEmail()) && !req.getOrdererEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email người đặt không đúng định dạng");
        }
        if (isBlank(req.getReceiverName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người nhận không được để trống");
        }
        if (isBlank(req.getReceiverPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại nhận hàng không được để trống");
        }
        if (isBlank(req.getShippingProvince())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỉnh / Thành phố không được để trống");
        }
        if (isBlank(req.getShippingDistrict())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quận / Huyện không được để trống");
        }
        if (isBlank(req.getShippingWard())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phường / Xã không được để trống");
        }
        if (isBlank(req.getShippingDetailAddress())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ chi tiết không được để trống");
        }
    }

    private String buildShippingAddressLine(CreateOrderRequest req) {
        if (!isBlank(req.getShippingAddressLine())) {
            return blankToNull(req.getShippingAddressLine());
        }
        return Arrays.asList(
                        blankToNull(req.getShippingDetailAddress()),
                        blankToNull(req.getShippingWard()),
                        blankToNull(req.getShippingDistrict()),
                        blankToNull(req.getShippingProvince())
                )
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    private void applyAdminShippingInfo(Order order, CreateOrderRequest req) {
        if (req.getChannel() == SalesChannel.ONLINE) {
            order.setOrdererName(blankToNull(req.getOrdererName()));
            order.setOrdererEmail(blankToNull(req.getOrdererEmail()));
            order.setOrdererPhone(blankToNull(req.getOrdererPhone()));
            order.setReceiverName(blankToNull(req.getReceiverName()));
            order.setReceiverPhone(blankToNull(req.getReceiverPhone()));
            order.setAddressLabel(blankToNull(req.getAddressLabel()));
            order.setShippingProvince(blankToNull(req.getShippingProvince()));
            order.setShippingDistrict(blankToNull(req.getShippingDistrict()));
            order.setShippingWard(blankToNull(req.getShippingWard()));
            order.setShippingDetailAddress(blankToNull(req.getShippingDetailAddress()));
            order.setShippingAddressLine(buildShippingAddressLine(req));
            return;
        }

        // Đơn tại quầy không yêu cầu thông tin giao hàng. Giữ các field này null để không gây hiểu nhầm là đơn ship.
        order.setOrdererName(null);
        order.setOrdererEmail(null);
        order.setOrdererPhone(null);
        order.setReceiverName(null);
        order.setReceiverPhone(null);
        order.setAddressLabel(null);
        order.setShippingProvince(null);
        order.setShippingDistrict(null);
        order.setShippingWard(null);
        order.setShippingDetailAddress(null);
        order.setShippingAddressLine(null);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private User getCurrentUserOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            String username = authentication.getName();
            if (username == null || username.trim().isEmpty() || "anonymousUser".equals(username)) {
                return null;
            }

            return userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private List<OrderItem> defaultItems(Order order) {
        return order.getItems() == null ? Collections.emptyList() : order.getItems();
    }

    private boolean matchKeyword(String source, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        if (source == null) return false;
        return source.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private boolean isBetween(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return false;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private boolean matchDateRange(LocalDateTime createdAt, Optional<LocalDate> dateFromOpt, Optional<LocalDate> dateToOpt) {
        if (createdAt == null) return false;
        LocalDate date = createdAt.toLocalDate();
        if (dateFromOpt.isPresent() && date.isBefore(dateFromOpt.get())) return false;
        if (dateToOpt.isPresent() && date.isAfter(dateToOpt.get())) return false;
        return true;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm phải là số nguyên dương.");
        }
    }

    private void assertEditable(Order order) {
        if (order.getOrderStatus() != OrderStatus.NEW && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể chỉnh sửa đơn hàng ở trạng thái hiện tại."
            );
        }
    }

    private void assertCancelable(Order order) {
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã hoàn thành, không thể cập nhật trạng thái.");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã hủy, không thể cập nhật trạng thái.");
        }
    }

    private static final int RETURN_ALLOWED_DAYS = 7;

    private void assertReturnable(Order order) {
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ đơn hàng đã hoàn thành mới được yêu cầu trả hàng."
            );
        }
        LocalDateTime completedAt = order.getCompletedAt() != null ? order.getCompletedAt() : order.getDeliveredAt();
        if (completedAt == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ đơn hàng đã hoàn thành mới được yêu cầu trả hàng."
            );
        }
        if (completedAt.plusDays(RETURN_ALLOWED_DAYS).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng đã quá thời hạn trả hàng."
            );
        }
    }

    // =========================================================================================
    // 🔥 CÁC HÀM XỬ LÝ KHO (ĐÃ ĐƯỢC TỐI ƯU HÓA)
    // =========================================================================================
    private void decreaseStock(Long variantId, int qty, String productName) {
        if (qty <= 0) return;
        ProductVariant variant = getVariantOr404(variantId);
        int current = variant.getStock();
        if (current < qty) {
            String name = (productName != null && !productName.isEmpty()) ? productName : ("SKU " + variant.getSku());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sản phẩm " + name + " không đủ tồn kho! Tồn hiện tại: " + current + ", Cần: " + qty
            );
        }
        variant.setStock(current - qty);
        if (variant.getStock() == 0) {
            variant.setStatus("Hết hàng");
        }
        productVariantRepository.save(variant);
    }

    private void increaseStock(Long variantId, int qty) {
        if (qty <= 0) return;
        ProductVariant variant = getVariantOr404(variantId);
        variant.setStock(variant.getStock() + qty);
        // Nếu trước đó đang Hết hàng mà được cộng trả lại -> Đổi thành Còn hàng
        if (variant.getStock() > 0 && "Hết hàng".equals(variant.getStatus())) {
            variant.setStatus("Còn hàng");
        }
        productVariantRepository.save(variant);
    }
    // =========================================================================================

    private BigDecimal resolveDefaultUnitPrice(ProductVariant variant) {
        try {
            if (variant.getSalePrice() != null && variant.getSalePrice().signum() > 0) {
                return variant.getSalePrice();
            }
        } catch (Exception ignored) {
        }
        try {
            if (variant.getPrice() != null && variant.getPrice().signum() > 0) {
                return variant.getPrice();
            }
        } catch (Exception ignored) {
        }
        return BigDecimal.ZERO;
    }

    private String buildVariantName(ProductVariant variant) {
        try {
            if (variant.getProduct() != null && variant.getProduct().getName() != null) {
                return variant.getProduct().getName();
            }
        } catch (Exception ignored) {
        }
        return "Variant#" + variant.getId();
    }

    private void calcLine(OrderItem it) {
        BigDecimal unit = nz(it.getUnitPrice());
        BigDecimal qty = BigDecimal.valueOf(nzInt(it.getQuantity()));
        BigDecimal discount = nz(it.getLineDiscountAmount());
        BigDecimal line = unit.multiply(qty).subtract(discount);
        if (line.signum() < 0) line = BigDecimal.ZERO;
        it.setLineTotalAmount(line);
    }

    private void recalcOrderTotals(Order order) {
        BigDecimal subtotal = nz(order.getSubtotalAmount());
        BigDecimal discount = nz(order.getDiscountAmount());
        BigDecimal ship = nz(order.getShippingFee());

        BigDecimal total = subtotal.subtract(discount).add(ship);
        if (total.signum() < 0) total = BigDecimal.ZERO;

        order.setTotalAmount(total);

        BigDecimal finalAmt = total.subtract(nz(order.getReturnedAmount()));
        if (finalAmt.signum() < 0) finalAmt = BigDecimal.ZERO;

        order.setFinalAmount(finalAmt);
    }

    private BigDecimal calcRevenue(Order o) {
        if (o == null) return BigDecimal.ZERO;
        if (o.getOrderStatus() == OrderStatus.COMPLETED) {
            return nz(o.getFinalAmount());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal sumRevenue(List<Order> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            total = total.add(calcRevenue(o));
        }
        return total;
    }

    private LocalDate getRevenueDate(Order o) {
        if (o.getCompletedAt() != null) return o.getCompletedAt().toLocalDate();
        if (o.getCreatedAt() != null) return o.getCreatedAt().toLocalDate();
        return null;
    }

    private String generateOrderCode() {
        String date = LocalDate.now().toString().replace("-", "");
        String rnd = String.valueOf(new Random().nextInt(900000) + 100000);
        String code = "ORD" + date + rnd;
        if (orderRepo.existsByOrderCode(code)) {
            return generateOrderCode();
        }
        return code;
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "CREATE", entity = "Order", description = "Đã tạo đơn hàng mới. Kênh: #{#req.channel}, Thanh toán: #{#req.paymentMethod}")
    public OrderDetailDTO create(CreateOrderRequest req) {
        validateAdminOnlineShippingInfo(req);

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setChannel(req.getChannel());
        order.setPaymentMethod(req.getPaymentMethod());
        order.setNote(req.getNote());
        applyAdminShippingInfo(order, req);

        if (req.getChannel() == SalesChannel.OFFLINE) {
            order.setShippingFee(BigDecimal.ZERO);
            // Case riêng OFFLINE + CASH: tạo đơn trước, admin xác nhận thanh toán sau.
            order.setOrderStatus(OrderStatus.NEW);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            order.setShippingStatus(com.sneakershop.backend.entity.order.enums.ShippingStatus.PENDING);
        } else {
            order.setShippingFee(nz(req.getShippingFee()));
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemCreateRequest ir : req.getItems()) {
            validateQuantity(ir.getQuantity());
            ProductVariant variant = getVariantOr404(ir.getVariantId());

            // Chuẩn hóa tồn kho: tạo đơn chỉ giữ chỗ, khi hoàn tất/giao thành công mới trừ stock thật.
            orderInventoryService.reserveStock(variant.getId(), ir.getQuantity(), "ORDER", order.getId(), "Giữ chỗ tồn kho khi tạo đơn admin.");

            OrderItem it = new OrderItem();
            it.setOrder(order);
            it.setVariant(variant);
            it.setQuantity(ir.getQuantity());
            it.setUnitPrice(ir.getUnitPrice() != null ? nz(ir.getUnitPrice()) : nz(resolveDefaultUnitPrice(variant)));
            it.setLineDiscountAmount(ir.getLineDiscountAmount() != null ? nz(ir.getLineDiscountAmount()) : BigDecimal.ZERO);
            it.setSkuSnapshot(ir.getSkuSnapshot() != null ? ir.getSkuSnapshot() : variant.getSku());
            it.setProductNameSnapshot(ir.getProductNameSnapshot() != null ? ir.getProductNameSnapshot() : buildVariantName(variant));
            snapshotOrderItem(it, variant);

            calcLine(it);
            items.add(it);
            subtotal = subtotal.add(nz(it.getLineTotalAmount()));
        }
        order.setItems(items);
        order.setSubtotalAmount(subtotal);

        BigDecimal shippingDiscount = BigDecimal.ZERO;
        String fsCode = "";
        if (req.getFreeShipVoucherId() != null) {
            Voucher fsVoucher = voucherRepository.findById(req.getFreeShipVoucherId()).orElse(null);
            if (fsVoucher != null && "ACTIVE".equals(fsVoucher.getStatus())) {
                BigDecimal maxFreeShip = BigDecimal.valueOf(fsVoucher.getValue() != null ? fsVoucher.getValue() : 0L);
                shippingDiscount = nz(order.getShippingFee()).min(maxFreeShip);
                fsCode = fsVoucher.getCode();
            }
        }

        BigDecimal voucherDiscount = BigDecimal.ZERO;
        String vCode = "";
        if (req.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(req.getVoucherId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher không tồn tại"));

            if (!"ACTIVE".equals(voucher.getStatus()) ||
                    (voucher.getQuantity() != null && voucher.getUsedCount() >= voucher.getQuantity())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher không hợp lệ hoặc đã hết lượt");
            }

            if ("PERCENT".equals(voucher.getType())) {
                BigDecimal percent = BigDecimal.valueOf(voucher.getValue() != null ? voucher.getValue() : 0L);
                voucherDiscount = subtotal.multiply(percent).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                if (voucher.getMaxDiscount() != null) {
                    voucherDiscount = voucherDiscount.min(BigDecimal.valueOf(voucher.getMaxDiscount()));
                }
            } else {
                voucherDiscount = BigDecimal.valueOf(voucher.getValue() != null ? voucher.getValue() : 0L);
            }
            vCode = voucher.getCode();
        }

        String combinedCodes = vCode;
        if (!fsCode.isEmpty()) {
            combinedCodes = combinedCodes.isEmpty() ? fsCode : combinedCodes + ", " + fsCode;
        }
        order.setVoucherCode(combinedCodes);

        order.setDiscountAmount(voucherDiscount.add(shippingDiscount).add(nz(req.getDiscountAmount())));

        if (req.getCustomerId() != null) {
            order.setCustomer(em.getReference(Customer.class, req.getCustomerId()));
        }
        User currentUser = getCurrentUserOrNull();
        if (currentUser != null) {
            order.setCreatedBy(currentUser);
        }

        recalcOrderTotals(order);

        Order savedOrder = orderRepo.save(order);

        if (req.getVoucherId() != null) {
            voucherService.useVoucher(req.getVoucherId(), req.getCustomerId(), savedOrder.getId(), voucherDiscount.doubleValue());
        }
        if (req.getFreeShipVoucherId() != null) {
            voucherService.useVoucher(req.getFreeShipVoucherId(), req.getCustomerId(), savedOrder.getId(), shippingDiscount.doubleValue());
        }

        return toDetailDTO(savedOrder);
    }

    private long nzLong(Long v) {
        return v == null ? 0L : v;
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> list(
            Optional<OrderStatus> statusOpt,
            Optional<SalesChannel> channelOpt,
            Optional<Long> customerIdOpt,
            Optional<Long> createdByIdOpt,
            Optional<LocalDate> dateFromOpt,
            Optional<LocalDate> dateToOpt,
            Optional<String> keywordOpt
    ) {
        return orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(o -> statusOpt.map(s -> s == o.getOrderStatus()).orElse(true))
                .filter(o -> channelOpt.map(c -> c == o.getChannel()).orElse(true))
                .filter(o -> customerIdOpt.map(id -> o.getCustomer() != null && Objects.equals(o.getCustomer().getId(), id)).orElse(true))
                .filter(o -> createdByIdOpt.map(id -> o.getCreatedBy() != null && Objects.equals(o.getCreatedBy().getId(), id)).orElse(true))
                .filter(o -> matchDateRange(o.getCreatedAt(), dateFromOpt, dateToOpt))
                .filter(o -> matchKeyword(o.getOrderCode(), keywordOpt.orElse(null)))
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> list(Optional<OrderStatus> statusOpt) {
        return list(statusOpt, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> listByDate(LocalDate date) {
        return orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isEqual(date))
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> listByCustomer(Long customerId) {
        return orderRepo.findAllByCustomer_IdAndDeletedFalseOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> listByStaff(Long createdById) {
        return orderRepo.findAllByCreatedBy_IdAndDeletedFalseOrderByCreatedAtDesc(createdById)
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailDTO detail(Long id) {
        return toDetailDTO(getOrderOr404(id));
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "UPDATE", entity = "Order", description = "Đã cập nhật thông tin đơn hàng ID: #{#id}")
    public OrderDetailDTO update(Long id, UpdateOrderRequest req) {
        Order order = getOrderOr404(id);
        assertEditable(order);

        if (req.getChannel() != null) order.setChannel(req.getChannel());
        if (req.getPaymentMethod() != null) order.setPaymentMethod(req.getPaymentMethod());
        if (req.getPaymentStatus() != null) order.setPaymentStatus(req.getPaymentStatus());

        if (req.getChannel() == SalesChannel.OFFLINE) {
            order.setShippingFee(BigDecimal.ZERO);
        } else if (req.getShippingFee() != null) {
            order.setShippingFee(nz(req.getShippingFee()));
        }

        if (req.getDiscountAmount() != null) order.setDiscountAmount(nz(req.getDiscountAmount()));
        if (req.getNote() != null) order.setNote(req.getNote());

        recalcOrderTotals(order);
        return toDetailDTO(orderRepo.save(order));
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "CANCEL", entity = "Order", description = "Đã hủy đơn hàng ID: #{#id}. Lý do: #{#req.reason}")
    public OrderDetailDTO cancel(Long id, CancelOrderRequest req) {
        Order order = getOrderOr404(id);
        assertCancelable(order);

        orderInventoryService.releaseForCancellation(order);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(req.getReason());
        order.setCancelledAt(LocalDateTime.now());

        if (req.getCancelledById() != null) {
            order.setCancelledBy(em.getReference(User.class, req.getCancelledById()));
        }

        return toDetailDTO(orderRepo.save(order));
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "UPDATE", entity = "Order", description = "Đã cập nhật trạng thái đơn hàng ID: #{#id} sang #{#req.status}")
    public OrderDetailDTO updateStatus(Long id, UpdateOrderStatusRequest req) {
        Order order = getOrderOr404(id);
        OrderStatus current = order.getOrderStatus();
        OrderStatus next = req.getStatus();

        if (next == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái đơn hàng không hợp lệ.");
        }
        if (current == OrderStatus.COMPLETED || current == OrderStatus.PARTIALLY_RETURNED || current == OrderStatus.RETURNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã hoàn thành, không thể cập nhật trạng thái.");
        }
        if (current == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã hủy, không thể cập nhật trạng thái.");
        }

        boolean valid =
                (current == OrderStatus.NEW && (next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED)) ||
                (current == OrderStatus.PROCESSING && (next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED)) ||
                (current == OrderStatus.SHIPPING && (next == OrderStatus.COMPLETED || next == OrderStatus.CANCELLED));

        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển trạng thái đơn hàng ở bước hiện tại.");
        }

        if (order.getChannel() == SalesChannel.ONLINE) {
            if (next == OrderStatus.PROCESSING && order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chuyển khoản chưa thanh toán đủ, không thể xác nhận xử lý.");
            }
            if (next == OrderStatus.SHIPPING && order.getShippingStatus() != ShippingStatus.READY_TO_SHIP && order.getShippingStatus() != ShippingStatus.SHIPPED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn online chỉ được chuyển sang đang giao khi đã sẵn sàng giao.");
            }
            if (next == OrderStatus.COMPLETED) {
                if (order.getShippingStatus() != ShippingStatus.DELIVERED) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn online chỉ được hoàn thành khi đã giao thành công.");
                }
                if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentStatus() != PaymentStatus.PAID) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chuyển khoản chưa thanh toán đủ, không thể hoàn thành.");
                }
            }
        }

        boolean completingNow = current != OrderStatus.COMPLETED && next == OrderStatus.COMPLETED;
        if (completingNow) {
            orderInventoryService.commitReservedStock(order);
            if (order.getCustomer() != null) {
                customerService.addPointsFromCompletedOrder(order.getCustomer().getId(), nz(order.getFinalAmount()), order.getOrderCode());
            }
        }

        if (next == OrderStatus.CANCELLED) {
            orderInventoryService.releaseForCancellation(order);
            order.setCancelledAt(LocalDateTime.now());
        }

        order.setOrderStatus(next);

        if (next == OrderStatus.SHIPPING) {
            if (order.getShippedAt() == null) order.setShippedAt(LocalDateTime.now());
            if (order.getShippingStatus() == null || order.getShippingStatus() == ShippingStatus.READY_TO_SHIP) {
                order.setShippingStatus(ShippingStatus.SHIPPED);
            }
        }
        if (next == OrderStatus.COMPLETED) {
            if (order.getCompletedAt() == null) order.setCompletedAt(LocalDateTime.now());
            if (order.getDeliveredAt() == null) order.setDeliveredAt(LocalDateTime.now());
            order.setShippingStatus(ShippingStatus.DELIVERED);
            if (order.getPaymentMethod() == PaymentMethod.COD && order.getPaymentStatus() != PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }

        if (completingNow) {
            telegramNotificationService.sendMessage(
                    "✅ <b>Đơn hàng hoàn thành</b>\n"
                            + "Mã đơn: <b>" + safe(order.getOrderCode()) + "</b>\n"
                            + "Khách: <b>" + safe(order.getOrdererName()) + "</b>\n"
                            + "Tổng tiền: <b>" + nz(order.getFinalAmount()) + " VND</b>\n"
                            + "Thanh toán: <b>" + String.valueOf(order.getPaymentStatus()) + "</b>"
            );
        }

        recalcOrderTotals(order);
        return toDetailDTO(orderRepo.save(order));
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "DELETE", entity = "Order", description = "Đã xóa (soft delete) đơn hàng ID: #{#id}")
    public void delete(Long id) {
        Order order = getOrderOr404(id);
        order.setDeleted(true);
        order.setDeletedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "UPDATE", entity = "Order", description = "Đã thêm sản phẩm vào đơn hàng ID: #{#orderId}")
    public OrderDetailDTO addItems(Long orderId, List<OrderItemCreateRequest> itemsReq) {
        Order order = getOrderOr404(orderId);
        assertEditable(order);

        for (OrderItemCreateRequest ir : itemsReq) {
            validateQuantity(ir.getQuantity());
            ProductVariant variant = getVariantOr404(ir.getVariantId());

            // Chuẩn hóa tồn kho: thêm sản phẩm vào đơn chỉ giữ chỗ.
            orderInventoryService.reserveStock(variant.getId(), ir.getQuantity(), "ORDER", order.getId(), "Giữ chỗ tồn kho khi thêm sản phẩm vào đơn.");

            OrderItem it = new OrderItem();
            it.setOrder(order);
            it.setVariant(variant);
            it.setQuantity(ir.getQuantity());
            it.setUnitPrice(ir.getUnitPrice() != null ? nz(ir.getUnitPrice()) : nz(resolveDefaultUnitPrice(variant)));
            it.setLineDiscountAmount(ir.getLineDiscountAmount() != null ? nz(ir.getLineDiscountAmount()) : BigDecimal.ZERO);
            it.setSkuSnapshot(ir.getSkuSnapshot() != null ? ir.getSkuSnapshot() : variant.getSku());
            it.setProductNameSnapshot(ir.getProductNameSnapshot() != null ? ir.getProductNameSnapshot() : buildVariantName(variant));
            snapshotOrderItem(it, variant);

            calcLine(it);
            order.getItems().add(it);
        }

        recalcOrderTotals(order);
        return toDetailDTO(orderRepo.save(order));
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "UPDATE", entity = "Order", description = "Đã cập nhật số lượng sản phẩm (Item ID: #{#itemId}) trong đơn hàng ID: #{#orderId}")
    public OrderDetailDTO updateItemQty(Long orderId, Long itemId, UpdateItemQuantityRequest req) {
        Order order = getOrderOr404(orderId);
        assertEditable(order);

        OrderItem item = order.getItems().stream()
                .filter(i -> Objects.equals(i.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found: " + itemId));

        validateQuantity(req.getQuantity());

        int oldQty = nzInt(item.getQuantity());
        int newQty = req.getQuantity();
        int diff = newQty - oldQty;

        // 🔥 FIX: BÙ/TRỪ KHO THÔNG MINH KHI ĐỔI SỐ LƯỢNG TRONG ĐƠN HÀNG
        if (diff > 0) {
            // Tăng số lượng: giữ chỗ thêm phần chênh lệch.
            orderInventoryService.reserveStock(item.getVariantId(), diff, "ORDER", order.getId(), "Giữ chỗ thêm khi tăng số lượng sản phẩm trong đơn.");
        } else if (diff < 0) {
            // Giảm số lượng: nhả phần giữ chỗ tương ứng.
            orderInventoryService.releaseReserved(item.getVariantId(), -diff, "ORDER", order.getId(), "Nhả giữ chỗ khi giảm số lượng sản phẩm trong đơn.");
        }

        item.setQuantity(req.getQuantity());
        calcLine(item);
        recalcOrderTotals(order);

        return toDetailDTO(orderRepo.save(order));
    }

    @Transactional
    @AuditAction(module = "ORDER", action = "RETURN", entity = "Order", description = "Đã xử lý trả hàng cho đơn ID: #{#orderId}. Trạng thái: #{#req.returnStatus}")
    public OrderDetailDTO applyReturn(Long orderId, ReturnOrderRequest req) {
        Order order = getOrderOr404(orderId);
        assertReturnable(order);

        OrderStatus originalOrderStatus = order.getOrderStatus();
        ReturnStatus previousReturnStatus = order.getReturnStatus();

        order.setReturnStatus(req.getReturnStatus());
        order.setReturnNote(req.getReturnNote());
        order.setReturnedAt(LocalDateTime.now());

        if (req.getItems() != null) {
            Map<Long, ReturnItemRequest> map = req.getItems().stream()
                    .collect(Collectors.toMap(ReturnItemRequest::getOrderItemId, x -> x, (a, b) -> b));

            for (OrderItem it : defaultItems(order)) {
                ReturnItemRequest r = map.get(it.getId());
                if (r == null) continue;

                int newReturned = Math.max(0, r.getReturnedQuantity());
                int maxQty = nzInt(it.getQuantity());
                if (newReturned > maxQty) newReturned = maxQty;

                it.setReturnedQuantity(newReturned);
                it.setReturnNote(r.getReturnNote());
                it.setReturnedAt(LocalDateTime.now());
            }
        }

        boolean completingReturnNow =
                req.getReturnStatus() == ReturnStatus.COMPLETED &&
                        previousReturnStatus != ReturnStatus.COMPLETED &&
                        originalOrderStatus == OrderStatus.COMPLETED;

        if (completingReturnNow) {
            orderInventoryService.restockReturnedItems(order);
        }

        BigDecimal returnedAmount = BigDecimal.ZERO;
        for (OrderItem it : defaultItems(order)) {
            int rq = nzInt(it.getReturnedQuantity());
            if (rq > 0) {
                BigDecimal qty = BigDecimal.valueOf(Math.max(1, nzInt(it.getQuantity())));
                BigDecimal unitNet = nz(it.getLineTotalAmount()).divide(qty, 2, RoundingMode.HALF_UP);
                if (unitNet.signum() < 0) unitNet = BigDecimal.ZERO;
                returnedAmount = returnedAmount.add(unitNet.multiply(BigDecimal.valueOf(rq)));
            }
        }

        if (req.getReturnedAmountOverride() != null) {
            returnedAmount = nz(req.getReturnedAmountOverride());
        }

        order.setReturnedAmount(returnedAmount);

        if (req.getReturnStatus() == ReturnStatus.COMPLETED && order.getOrderStatus() == OrderStatus.COMPLETED) {
            boolean allReturned = defaultItems(order).stream().allMatch(i -> nzInt(i.getReturnedQuantity()) >= nzInt(i.getQuantity()));
            order.setOrderStatus(allReturned ? OrderStatus.RETURNED : OrderStatus.PARTIALLY_RETURNED);
        }

        recalcOrderTotals(order);
        if (order.getReturnedAmount() != null && order.getReturnedAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (order.getReturnedAmount().compareTo(order.getTotalAmount()) >= 0) {
                order.setPaymentStatus(PaymentStatus.REFUNDED); // Hoàn toàn bộ
            } else {
                order.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED); // Hoàn một phần
            }
        }
        // TÍCH ĐIỂM CHO TRƯỜNG HỢP HOÀN TẤT ĐƠN THÔNG QUA LUỒNG TRẢ HÀNG
        if (req.getReturnStatus() == ReturnStatus.COMPLETED && order.getOrderStatus() == OrderStatus.COMPLETED && originalOrderStatus != OrderStatus.COMPLETED) {
            if (order.getCustomer() != null) {
                customerService.addPointsFromCompletedOrder(order.getCustomer().getId(), nz(order.getFinalAmount()), order.getOrderCode());
            }
        }

        return toDetailDTO(orderRepo.save(order));
    }

    @Transactional(readOnly = true)
    public List<ReturnReportDTO> returnReport(Optional<ReturnStatus> statusOpt) {
        List<Order> orders = statusOpt
                .map(s -> orderRepo.findAllByReturnStatusAndDeletedFalseOrderByCreatedAtDesc(s))
                .orElseGet(orderRepo::findAllByReturnStatusIsNotNullAndDeletedFalseOrderByCreatedAtDesc);

        return orders.stream().map(o -> {
            ReturnReportDTO r = new ReturnReportDTO();
            r.setOrderId(o.getId());
            r.setOrderCode(o.getOrderCode());
            r.setReturnStatus(o.getReturnStatus());
            r.setTotalAmount(o.getTotalAmount());
            r.setReturnedAmount(o.getReturnedAmount());
            r.setFinalAmount(o.getFinalAmount());
            r.setReturnedAt(o.getReturnedAt());
            return r;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StaffOrderStatisticDTO> statsByStaff() {
        Map<Long, StaffOrderStatisticDTO> map = new LinkedHashMap<>();
        for (Order o : orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            Long staffId = o.getCreatedBy() != null ? o.getCreatedBy().getId() : 0L;
            StaffOrderStatisticDTO dto = map.computeIfAbsent(staffId, k -> {
                StaffOrderStatisticDTO x = new StaffOrderStatisticDTO();
                x.setCreatedById(staffId == 0L ? null : staffId);
                x.setOrderCount(0L);
                x.setCompletedCount(0L);
                x.setCancelledCount(0L);
                x.setRevenue(BigDecimal.ZERO);
                return x;
            });

            dto.setOrderCount(dto.getOrderCount() + 1);
            if (o.getOrderStatus() == OrderStatus.COMPLETED) {
                dto.setCompletedCount(dto.getCompletedCount() + 1);
                dto.setRevenue(nz(dto.getRevenue()).add(calcRevenue(o)));
            }
            if (o.getOrderStatus() == OrderStatus.CANCELLED) {
                dto.setCancelledCount(dto.getCancelledCount() + 1);
            }
        }
        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<CustomerRevenueDTO> revenueByCustomer() {
        Map<Long, CustomerRevenueDTO> map = new LinkedHashMap<>();
        for (Order o : orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            if (o.getOrderStatus() != OrderStatus.COMPLETED) continue;
            Long customerId = o.getCustomer() != null ? o.getCustomer().getId() : 0L;
            CustomerRevenueDTO dto = map.computeIfAbsent(customerId, k -> {
                CustomerRevenueDTO x = new CustomerRevenueDTO();
                x.setCustomerId(customerId == 0L ? null : customerId);
                x.setOrderCount(0L);
                x.setRevenue(BigDecimal.ZERO);
                return x;
            });
            dto.setOrderCount(dto.getOrderCount() + 1);
            dto.setRevenue(nz(dto.getRevenue()).add(calcRevenue(o)));
        }
        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<DailyRevenueDTO> revenueDaily(LocalDate dateFrom, LocalDate dateTo) {
        Map<LocalDate, DailyRevenueDTO> map = new TreeMap<>();
        for (Order o : orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            if (o.getOrderStatus() != OrderStatus.COMPLETED) continue;
            LocalDate date = getRevenueDate(o);
            if (!isBetween(date, dateFrom, dateTo)) continue;

            DailyRevenueDTO dto = map.computeIfAbsent(date, d -> {
                DailyRevenueDTO x = new DailyRevenueDTO();
                x.setDate(d);
                x.setOrderCount(0L);
                x.setRevenue(BigDecimal.ZERO);
                return x;
            });
            dto.setOrderCount(dto.getOrderCount() + 1);
            dto.setRevenue(nz(dto.getRevenue()).add(calcRevenue(o)));
        }
        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<WeeklyRevenueDTO> revenueWeekly(LocalDate dateFrom, LocalDate dateTo) {
        WeekFields wf = WeekFields.ISO;
        Map<String, WeeklyRevenueDTO> map = new TreeMap<>();

        for (Order o : orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            if (o.getOrderStatus() != OrderStatus.COMPLETED) continue;
            LocalDate date = getRevenueDate(o);
            if (!isBetween(date, dateFrom, dateTo)) continue;

            int week = date.get(wf.weekOfWeekBasedYear());
            int year = date.get(wf.weekBasedYear());
            String key = year + "-W" + String.format("%02d", week);

            WeeklyRevenueDTO dto = map.computeIfAbsent(key, k -> {
                WeeklyRevenueDTO x = new WeeklyRevenueDTO();
                x.setWeekLabel(k);
                x.setOrderCount(0L);
                x.setRevenue(BigDecimal.ZERO);
                return x;
            });
            dto.setOrderCount(dto.getOrderCount() + 1);
            dto.setRevenue(nz(dto.getRevenue()).add(calcRevenue(o)));
        }

        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueDTO> revenueMonthly(LocalDate dateFrom, LocalDate dateTo) {
        Map<String, MonthlyRevenueDTO> map = new TreeMap<>();

        for (Order o : orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            if (o.getOrderStatus() != OrderStatus.COMPLETED) continue;
            LocalDate date = getRevenueDate(o);
            if (!isBetween(date, dateFrom, dateTo)) continue;

            String key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());

            MonthlyRevenueDTO dto = map.computeIfAbsent(key, k -> {
                MonthlyRevenueDTO x = new MonthlyRevenueDTO();
                x.setMonth(k);
                x.setOrderCount(0L);
                x.setRevenue(BigDecimal.ZERO);
                return x;
            });
            dto.setOrderCount(dto.getOrderCount() + 1);
            dto.setRevenue(nz(dto.getRevenue()).add(calcRevenue(o)));
        }

        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<BestSellingProductDTO> bestSellingProducts() {
        Map<Long, BestSellingProductDTO> map = new LinkedHashMap<>();

        for (Order o : orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            if (o.getOrderStatus() == OrderStatus.CANCELLED) continue;

            for (OrderItem it : defaultItems(o)) {
                Long variantId = it.getVariantId() != null ? it.getVariantId() : 0L;
                BestSellingProductDTO dto = map.computeIfAbsent(variantId, k -> {
                    BestSellingProductDTO x = new BestSellingProductDTO();
                    x.setVariantId(variantId == 0L ? null : variantId);
                    x.setSkuSnapshot(it.getSkuSnapshot());
                    x.setProductNameSnapshot(it.getProductNameSnapshot());
                    x.setTotalQuantity(0L);
                    x.setReturnedQuantity(0L);
                    x.setNetQuantity(0L);
                    x.setRevenue(BigDecimal.ZERO);
                    return x;
                });

                long qty = nzInt(it.getQuantity());
                long returnedQty = nzInt(it.getReturnedQuantity());
                long netQty = Math.max(0, qty - returnedQty);

                dto.setTotalQuantity(dto.getTotalQuantity() + qty);
                dto.setReturnedQuantity(dto.getReturnedQuantity() + returnedQty);
                dto.setNetQuantity(dto.getNetQuantity() + netQty);

                if (o.getOrderStatus() == OrderStatus.COMPLETED) {
                    BigDecimal qtyBd = BigDecimal.valueOf(Math.max(1, nzInt(it.getQuantity())));
                    BigDecimal unitNet = nz(it.getLineTotalAmount()).divide(qtyBd, 2, RoundingMode.HALF_UP);
                    dto.setRevenue(nz(dto.getRevenue()).add(unitNet.multiply(BigDecimal.valueOf(netQty))));
                }
            }
        }

        return map.values().stream()
                .sorted(Comparator.comparing(BestSellingProductDTO::getNetQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnedProductStatisticDTO> returnedProducts() {
        Map<Long, ReturnedProductStatisticDTO> map = new LinkedHashMap<>();

        for (Order o : orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            for (OrderItem it : defaultItems(o)) {
                int returnedQty = nzInt(it.getReturnedQuantity());
                if (returnedQty <= 0) continue;

                Long variantId = it.getVariantId() != null ? it.getVariantId() : 0L;
                ReturnedProductStatisticDTO dto = map.computeIfAbsent(variantId, k -> {
                    ReturnedProductStatisticDTO x = new ReturnedProductStatisticDTO();
                    x.setVariantId(variantId == 0L ? null : variantId);
                    x.setSkuSnapshot(it.getSkuSnapshot());
                    x.setProductNameSnapshot(it.getProductNameSnapshot());
                    x.setReturnedQuantity(0L);
                    x.setReturnedAmount(BigDecimal.ZERO);
                    return x;
                });

                dto.setReturnedQuantity(dto.getReturnedQuantity() + returnedQty);

                BigDecimal qtyBd = BigDecimal.valueOf(Math.max(1, nzInt(it.getQuantity())));
                BigDecimal unitNet = nz(it.getLineTotalAmount()).divide(qtyBd, 2, RoundingMode.HALF_UP);
                dto.setReturnedAmount(nz(dto.getReturnedAmount()).add(unitNet.multiply(BigDecimal.valueOf(returnedQty))));
            }
        }

        return map.values().stream()
                .sorted(Comparator.comparing(ReturnedProductStatisticDTO::getReturnedQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDashboardDTO dashboard() {
        OrderDashboardDTO dto = new OrderDashboardDTO();

        List<Order> all = orderRepo.findAllByDeletedFalseOrderByCreatedAtDesc();
        List<Order> completed = all.stream().filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED).collect(Collectors.toList());
        List<Order> todayOrders = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isEqual(LocalDate.now()))
                .collect(Collectors.toList());
        List<Order> monthOrders = all.stream()
                .filter(o -> o.getCreatedAt() != null
                        && o.getCreatedAt().getYear() == LocalDate.now().getYear()
                        && o.getCreatedAt().getMonthValue() == LocalDate.now().getMonthValue())
                .collect(Collectors.toList());

        dto.setTotalOrders((long) all.size());
        dto.setNewOrders(all.stream().filter(o -> o.getOrderStatus() == OrderStatus.NEW).count());
        dto.setProcessingOrders(all.stream().filter(o -> o.getOrderStatus() == OrderStatus.PROCESSING).count());
        dto.setShippingOrders(all.stream().filter(o -> o.getOrderStatus() == OrderStatus.SHIPPING).count());
        dto.setCompletedOrders(all.stream().filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED).count());
        dto.setCancelledOrders(all.stream().filter(o -> o.getOrderStatus() == OrderStatus.CANCELLED).count());
        dto.setReturnedOrders(all.stream().filter(o -> o.getReturnStatus() != null && o.getReturnStatus() != ReturnStatus.NONE).count());

        dto.setRevenueToday(sumRevenue(todayOrders));
        dto.setRevenueThisMonth(sumRevenue(monthOrders));
        dto.setTotalRevenue(sumRevenue(completed));

        long totalReturnedQty = all.stream()
                .flatMap(o -> defaultItems(o).stream())
                .mapToLong(i -> nzInt(i.getReturnedQuantity()))
                .sum();
        dto.setTotalReturnedQuantity(totalReturnedQty);

        dto.setTopProducts(bestSellingProducts().stream().limit(10).collect(Collectors.toList()));
        dto.setTopReturnedProducts(returnedProducts().stream().limit(10).collect(Collectors.toList()));
        dto.setRevenueByCustomer(revenueByCustomer().stream().limit(10).collect(Collectors.toList()));
        dto.setRevenueDaily(revenueDaily(null, null));
        dto.setRevenueMonthly(revenueMonthly(null, null));

        return dto;
    }

    @Transactional(readOnly = true)
    public String buildPrintHtml(Long id) {
        OrderDetailDTO dto = detail(id);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'/>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;padding:20px;color:#111}")
                .append("table{border-collapse:collapse;width:100%;margin-top:12px}")
                .append("td,th{border:1px solid #ccc;padding:8px;text-align:left}")
                .append(".title{font-size:22px;font-weight:bold;margin-bottom:10px}")
                .append(".meta p{margin:4px 0}")
                .append("</style>")
                .append("</head><body>");

        html.append("<div class='title'>Hóa đơn ").append(dto.getOrderCode()).append("</div>");
        html.append("<div class='meta'>");
        html.append("<p>Trạng thái đơn: ").append(dto.getOrderStatus()).append("</p>");
        html.append("<p>Phương thức thanh toán: ").append(dto.getPaymentMethod()).append("</p>");
        html.append("<p>Trạng thái thanh toán: ").append(dto.getPaymentStatus()).append("</p>");
        html.append("<p>Kênh bán: ").append(dto.getChannel()).append("</p>");
        html.append("</div>");

        html.append("<table><thead><tr>")
                .append("<th>Sản phẩm</th>")
                .append("<th>SKU</th>")
                .append("<th>Số lượng</th>")
                .append("<th>Đơn giá</th>")
                .append("<th>Giảm dòng</th>")
                .append("<th>Thành tiền</th>")
                .append("<th>Đã trả</th>")
                .append("</tr></thead><tbody>");

        if (dto.getItems() != null) {
            for (OrderItemDTO it : dto.getItems()) {
                html.append("<tr>")
                        .append("<td>").append(it.getProductNameSnapshot() == null ? "" : it.getProductNameSnapshot()).append("</td>")
                        .append("<td>").append(it.getSkuSnapshot() == null ? "" : it.getSkuSnapshot()).append("</td>")
                        .append("<td>").append(it.getQuantity()).append("</td>")
                        .append("<td>").append(it.getUnitPrice()).append("</td>")
                        .append("<td>").append(it.getLineDiscountAmount()).append("</td>")
                        .append("<td>").append(it.getLineTotalAmount()).append("</td>")
                        .append("<td>").append(it.getReturnedQuantity()).append("</td>")
                        .append("</tr>");
            }
        }

        html.append("</tbody></table>");
        html.append("<p>Subtotal: ").append(dto.getSubtotalAmount()).append("</p>");
        html.append("<p>Discount: ").append(dto.getDiscountAmount()).append("</p>");
        html.append("<p>Shipping: ").append(dto.getShippingFee()).append("</p>");
        html.append("<p>Total: ").append(dto.getTotalAmount()).append("</p>");
        html.append("<p>Returned: ").append(dto.getReturnedAmount()).append("</p>");
        html.append("<p><b>Final: ").append(dto.getFinalAmount()).append("</b></p>");
        html.append("</body></html>");
        return html.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportSingleOrderPdf(Long id) {
        OrderDetailDTO dto = detail(id);
        StringBuilder sb = new StringBuilder();
        sb.append("ORDER: ").append(dto.getOrderCode()).append("\n")
                .append("STATUS: ").append(dto.getOrderStatus()).append("\n")
                .append("CHANNEL: ").append(dto.getChannel()).append("\n")
                .append("PAYMENT: ").append(dto.getPaymentMethod()).append(" / ").append(dto.getPaymentStatus()).append("\n")
                .append("--------------------------------------------------\n");

        if (dto.getItems() != null) {
            for (OrderItemDTO it : dto.getItems()) {
                sb.append(it.getProductNameSnapshot()).append(" | SKU=").append(it.getSkuSnapshot())
                        .append(" | qty=").append(it.getQuantity())
                        .append(" | unit=").append(it.getUnitPrice())
                        .append(" | total=").append(it.getLineTotalAmount())
                        .append(" | returned=").append(it.getReturnedQuantity())
                        .append("\n");
            }
        }

        sb.append("--------------------------------------------------\n")
                .append("Subtotal: ").append(dto.getSubtotalAmount()).append("\n")
                .append("Discount: ").append(dto.getDiscountAmount()).append("\n")
                .append("Shipping: ").append(dto.getShippingFee()).append("\n")
                .append("Total: ").append(dto.getTotalAmount()).append("\n")
                .append("Returned: ").append(dto.getReturnedAmount()).append("\n")
                .append("Final: ").append(dto.getFinalAmount()).append("\n");

        return buildSimplePdf(sb.toString());
    }

    @Transactional(readOnly = true)
    public byte[] exportOrdersPdf(
            Optional<OrderStatus> statusOpt,
            Optional<SalesChannel> channelOpt,
            Optional<Long> customerIdOpt,
            Optional<Long> createdByIdOpt,
            Optional<LocalDate> dateFromOpt,
            Optional<LocalDate> dateToOpt,
            Optional<String> keywordOpt
    ) {
        List<OrderSummaryDTO> list = list(statusOpt, channelOpt, customerIdOpt, createdByIdOpt, dateFromOpt, dateToOpt, keywordOpt);

        StringBuilder sb = new StringBuilder();
        sb.append("ORDERS REPORT\n");
        sb.append("Rows: ").append(list.size()).append("\n");
        sb.append("--------------------------------------------------\n");
        for (OrderSummaryDTO dto : list) {
            sb.append(dto.getOrderCode())
                    .append(" | status=").append(dto.getOrderStatus())
                    .append(" | channel=").append(dto.getChannel())
                    .append(" | customer=").append(dto.getCustomerId())
                    .append(" | staff=").append(dto.getCreatedById())
                    .append(" | total=").append(dto.getTotalAmount())
                    .append(" | final=").append(dto.getFinalAmount())
                    .append(" | created=").append(dto.getCreatedAt())
                    .append("\n");
        }
        return buildSimplePdf(sb.toString());
    }

    @Transactional(readOnly = true)
    public OrderEmailPreviewDTO emailPreview(Long id) {
        Order order = getOrderOr404(id);

        OrderEmailPreviewDTO dto = new OrderEmailPreviewDTO();
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setToEmail(
                order.getCustomer() != null && order.getCustomer().getEmail() != null
                        ? order.getCustomer().getEmail()
                        : "customer@example.com"
        );
        dto.setSubject("Xác nhận đơn hàng " + order.getOrderCode());
        dto.setMarkedSent(Boolean.TRUE.equals(order.getEmailSent()));
        dto.setContent(buildEmailContent(order));
        return dto;
    }

    @Transactional
    public OrderEmailPreviewDTO markEmailSent(Long id) {
        Order order = getOrderOr404(id);
        order.setEmailSent(true);
        order.setEmailSentAt(LocalDateTime.now());
        orderRepo.save(order);
        return emailPreview(id);
    }

    private String buildEmailContent(Order order) {
        return "Xin chào,\n\nĐơn hàng " + order.getOrderCode()
                + " đã được tạo thành công.\n"
                + "Trạng thái hiện tại: " + order.getOrderStatus() + "\n"
                + "Tổng tiền: " + order.getTotalAmount() + "\n"
                + "Giá trị cuối cùng: " + order.getFinalAmount() + "\n\n"
                + "Cảm ơn bạn đã mua hàng.";
    }

    private byte[] buildSimplePdf(String text) {
        try {
            String safe = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            String[] lines = safe.split("\\r?\\n");

            StringBuilder stream = new StringBuilder();
            stream.append("BT\n/F1 10 Tf\n14 TL\n50 780 Td\n");
            for (int i = 0; i < lines.length; i++) {
                if (i == 0) {
                    stream.append("(").append(lines[i]).append(") Tj\n");
                } else {
                    stream.append("T*\n(").append(lines[i]).append(") Tj\n");
                }
            }
            stream.append("ET");

            byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();

            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));

            offsets.add(out.size());
            out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            offsets.add(out.size());
            out.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            offsets.add(out.size());
            out.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            offsets.add(out.size());
            out.write(("4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(streamBytes);
            out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            offsets.add(out.size());
            out.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            int xrefPos = out.size();
            out.write(("xref\n0 " + (offsets.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (Integer offset : offsets) {
                out.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            }

            out.write(("trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefPos + "\n%%EOF")
                    .getBytes(StandardCharsets.ISO_8859_1));

            return out.toByteArray();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot generate PDF");
        }
    }

    private OrderSummaryDTO toSummaryDTO(Order o) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setId(o.getId());
        dto.setOrderCode(o.getOrderCode());
        dto.setCustomerId(o.getCustomer() == null ? null : o.getCustomer().getId());
        dto.setCreatedById(o.getCreatedBy() == null ? null : o.getCreatedBy().getId());
        dto.setChannel(o.getChannel());
        dto.setOrderStatus(o.getOrderStatus());
        dto.setPaymentStatus(o.getPaymentStatus());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setFinalAmount(o.getFinalAmount());
        dto.setRevenue(calcRevenue(o));
        dto.setCreatedAt(o.getCreatedAt());
        return dto;
    }

    private OrderDetailDTO toDetailDTO(Order o) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(o.getId());
        dto.setOrderCode(o.getOrderCode());

        dto.setCustomerId(o.getCustomer() == null ? null : o.getCustomer().getId());
        dto.setCreatedById(o.getCreatedBy() == null ? null : o.getCreatedBy().getId());

        dto.setChannel(o.getChannel());
        dto.setOrderStatus(o.getOrderStatus());
        dto.setPaymentStatus(o.getPaymentStatus());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setReturnStatus(o.getReturnStatus());

        dto.setSubtotalAmount(o.getSubtotalAmount());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setShippingFee(o.getShippingFee());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setReturnedAmount(o.getReturnedAmount());
        dto.setFinalAmount(o.getFinalAmount());
        dto.setRevenue(calcRevenue(o));

        dto.setNote(o.getNote());
        dto.setReturnNote(o.getReturnNote());

        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());
        dto.setShippedAt(o.getShippedAt());
        dto.setCompletedAt(o.getCompletedAt());
        dto.setCancelledAt(o.getCancelledAt());
        dto.setReturnedAt(o.getReturnedAt());
        dto.setEmailSent(o.getEmailSent());
        dto.setEmailSentAt(o.getEmailSentAt());

        List<OrderItemDTO> items = new ArrayList<>();
        for (OrderItem it : defaultItems(o)) {
            OrderItemDTO itDto = new OrderItemDTO();
            itDto.setId(it.getId());
            itDto.setVariantId(it.getVariantId());
            itDto.setSkuSnapshot(it.getSkuSnapshot());
            itDto.setProductNameSnapshot(it.getProductNameSnapshot());
            itDto.setUnitPrice(it.getUnitPrice());
            itDto.setQuantity(it.getQuantity());
            itDto.setLineDiscountAmount(it.getLineDiscountAmount());
            itDto.setLineTotalAmount(it.getLineTotalAmount());
            itDto.setReturnedQuantity(it.getReturnedQuantity());
            itDto.setReturnNote(it.getReturnNote());
            items.add(itDto);
        }
        dto.setItems(items);

        return dto;
    }
}