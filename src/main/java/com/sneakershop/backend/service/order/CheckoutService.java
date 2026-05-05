package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.CheckoutPreviewRequest;
import com.sneakershop.backend.dto.order.CheckoutPreviewResponse;
import com.sneakershop.backend.dto.order.CheckoutRequest;
import com.sneakershop.backend.dto.order.CheckoutResponse;
import com.sneakershop.backend.dto.order.PaymentInitResponse;
import com.sneakershop.backend.dto.pricing.PriceResultDTO;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.order.Cart;
import com.sneakershop.backend.entity.order.CartItem;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.order.PaymentTransaction;
import com.sneakershop.backend.entity.order.enums.CartStatus;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import com.sneakershop.backend.entity.order.enums.TransactionStatus;
import com.sneakershop.backend.entity.order.enums.TransactionType;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.voucher.Voucher;
import com.sneakershop.backend.entity.voucher.VoucherUsage;
import com.sneakershop.backend.exception.OutOfStockException;
import com.sneakershop.backend.exception.VoucherInvalidException;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.order.CartRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.order.PaymentTransactionRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.repository.voucher.VoucherCustomerRepository;
import com.sneakershop.backend.repository.voucher.VoucherRepository;
import com.sneakershop.backend.repository.voucher.VoucherUsageRepository;
import com.sneakershop.backend.service.notification.TelegramNotificationService;
import com.sneakershop.backend.service.pricing.ProductPricingPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProductPricingPromotionService promotionPricingService;
    private final CustomerRepository customerRepository;
    private final List<PaymentGatewayClient> paymentGatewayClients;
    private final VoucherCustomerRepository voucherCustomerRepository;

    private final SepayService sepayService;
    private final TelegramNotificationService telegramNotificationService;
    private final OrderInventoryService orderInventoryService;

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(CheckoutPreviewRequest request) {
        Cart cart = resolveCart(request.getCartId(), request.getCustomerId(), request.getSessionKey());
        validateCart(cart);

        Customer customer = resolveCustomer(request.getCustomerId());

        List<CartItem> selectedItems = cart.getItems().stream()
                .filter(i -> Boolean.TRUE.equals(i.getSelected()))
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng không có sản phẩm được chọn");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal promotionDiscountTotal = BigDecimal.ZERO;

        for (CartItem cartItem : selectedItems) {
            ProductVariant variant = productVariantRepository.findById(cartItem.getVariant().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm: " + cartItem.getVariant().getId()));

            validateStock(variant, cartItem.getQuantity());

            PriceSnapshot priceSnapshot = resolveCheckoutPrice(variant, cartItem.getQuantity());
            BigDecimal promotionDiscount = priceSnapshot.getPromotionDiscount();
            BigDecimal finalLineTotal = priceSnapshot.getFinalLineTotal();

            subtotal = subtotal.add(finalLineTotal);
            promotionDiscountTotal = promotionDiscountTotal.add(promotionDiscount);
        }

        Order tempOrder = new Order();
        fillPreviewOrderContact(tempOrder, cart, customer);

        Voucher voucher = null;
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = validateAndResolveVoucher(request.getVoucherCode(), customer, tempOrder, subtotal);
            voucherDiscount = calculateVoucherDiscount(voucher, subtotal);
        }

        Voucher freeShipVoucher = null;
        BigDecimal shippingDiscount = BigDecimal.ZERO;

        // Truyền Tỉnh/Thành phố vào để tính ship
        BigDecimal shippingFee = calculateShippingFee(request.getShippingProvince(), subtotal);

        // Tính toán voucher freeship (Nếu Frontend gửi lên)
        if (request.getFreeShipVoucherCode() != null && !request.getFreeShipVoucherCode().isBlank()) {
            freeShipVoucher = validateAndResolveVoucher(request.getFreeShipVoucherCode(), customer, tempOrder, subtotal);
            shippingDiscount = calculateVoucherDiscount(freeShipVoucher, shippingFee); // Giảm tối đa bằng phí ship
        }

        BigDecimal discountAmount = promotionDiscountTotal
                .add(voucherDiscount)
                .add(shippingDiscount);
        BigDecimal totalAmount = subtotal.subtract(voucherDiscount).add(shippingFee).subtract(shippingDiscount);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        int totalItems = selectedItems.stream()
                .map(CartItem::getQuantity)
                .filter(q -> q != null && q > 0)
                .reduce(0, Integer::sum);

        return CheckoutPreviewResponse.builder()
                .cartId(cart.getId())
                .customerId(customer != null ? customer.getId() : null)
                .sessionKey(cart.getSessionKey())
                .totalItems(totalItems)
                .selectedItemCount(totalItems)
                .subtotalAmount(subtotal)
                .promotionDiscountAmount(promotionDiscountTotal)
                .voucherDiscountAmount(voucherDiscount)
                .shippingDiscountAmount(shippingDiscount)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .finalAmount(totalAmount)
                .voucherCode(voucher != null ? voucher.getCode() : null)
                .freeShipVoucherCode(freeShipVoucher != null ? freeShipVoucher.getCode() : null)
                .message("Preview checkout thành công")
                .build();
    }

    private void validateVoucherCustomerScope(Voucher voucher, Customer customer) {
        if (voucher == null) {
            throw new VoucherInvalidException("Voucher không tồn tại");
        }

        if (Boolean.TRUE.equals(voucher.getIsPublic())) {
            return;
        }

        if (customer == null) {
            throw new VoucherInvalidException(
                    "Voucher này chỉ áp dụng cho khách hàng được chỉ định. Vui lòng đăng nhập tài khoản phù hợp."
            );
        }

        boolean allowed = voucherCustomerRepository.existsByVoucher_IdAndCustomer_Id(
                voucher.getId(),
                customer.getId()
        );

        if (!allowed) {
            throw new VoucherInvalidException(
                    "Tài khoản của bạn không thuộc nhóm khách hàng được áp dụng voucher này."
            );
        }
    }

    private int getAvailableQuantity(ProductVariant variant) {
        return orderInventoryService.getAvailableQuantity(variant);
    }

    private void reserveStock(ProductVariant variant, int quantity) {
        orderInventoryService.reserveStock(variant, quantity);
    }

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        Cart cart = resolveCart(request.getCartId(), request.getCustomerId(), request.getSessionKey());
        validateCart(cart);

        Customer customer = resolveCustomer(request.getCustomerId());

        List<CartItem> selectedItems = cart.getItems().stream()
                .filter(i -> Boolean.TRUE.equals(i.getSelected()))
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng không có sản phẩm được chọn");
        }

        Order order = new Order();
        fillOrderHeader(order, request, cart, customer);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal promotionDiscountTotal = BigDecimal.ZERO;

        for (CartItem cartItem : selectedItems) {
            ProductVariant variant = productVariantRepository.findById(cartItem.getVariant().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy biến thể sản phẩm: " + cartItem.getVariant().getId()
                    ));

            validateStock(variant, cartItem.getQuantity());

            PriceSnapshot priceSnapshot = resolveCheckoutPrice(variant, cartItem.getQuantity());
            BigDecimal promotionDiscount = priceSnapshot.getPromotionDiscount();
            BigDecimal finalLineTotal = priceSnapshot.getFinalLineTotal();

            BigDecimal unitPriceAfterPromotion = cartItem.getQuantity() == null || cartItem.getQuantity() <= 0
                    ? BigDecimal.ZERO
                    : finalLineTotal.divide(BigDecimal.valueOf(cartItem.getQuantity()), 2, RoundingMode.HALF_UP);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setVariant(variant);
            snapshotItem(item, variant);
            item.setBaseUnitPrice(resolveBaseUnitPrice(variant));
            item.setUnitPrice(unitPriceAfterPromotion);
            item.setQuantity(cartItem.getQuantity());
            item.setPromotionDiscountAmount(promotionDiscount);
            item.setLineDiscountAmount(BigDecimal.ZERO);
            item.setLineTotalAmount(finalLineTotal);
            order.getItems().add(item);

            subtotal = subtotal.add(finalLineTotal);
            promotionDiscountTotal = promotionDiscountTotal.add(promotionDiscount);

            reserveStock(variant, cartItem.getQuantity());
        }

        Voucher voucher = null;
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = validateAndResolveVoucher(request.getVoucherCode(), customer, order, subtotal);
            voucherDiscount = calculateVoucherDiscount(voucher, subtotal);
        }

        // Tính phí ship và voucher freeship
        BigDecimal shippingFee = calculateShippingFee(request.getShippingProvince(), subtotal);

        Voucher freeShipVoucher = null;
        BigDecimal shippingDiscount = BigDecimal.ZERO;
        if (request.getFreeShipVoucherCode() != null && !request.getFreeShipVoucherCode().isBlank()) {
            freeShipVoucher = validateAndResolveVoucher(request.getFreeShipVoucherCode(), customer, order, subtotal);
            shippingDiscount = calculateVoucherDiscount(freeShipVoucher, shippingFee);
        }

        BigDecimal totalAmount = subtotal.subtract(voucherDiscount).add(shippingFee).subtract(shippingDiscount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        order.setVoucher(voucher);
        order.setVoucherCode(voucher != null ? voucher.getCode() : null);
        order.setVoucherNameSnapshot(voucher != null ? voucher.getName() : null);
        order.setVoucherTypeSnapshot(voucher != null ? voucher.getType() : null);
        order.setVoucherValueSnapshot(voucher != null ? voucher.getValue() : null);

        order.setSubtotalAmount(subtotal);
        order.setPromotionDiscountAmount(promotionDiscountTotal);
        order.setVoucherDiscountAmount(voucherDiscount);
        order.setShippingDiscountAmount(shippingDiscount);

        order.setDiscountAmount(nz(order.getPromotionDiscountAmount())
                .add(nz(order.getVoucherDiscountAmount()))
                .add(nz(order.getShippingDiscountAmount()))
                .add(nz(order.getManualDiscountAmount())));

        order.setShippingFee(shippingFee);
        order.setTotalAmount(totalAmount);
        order.setReturnedAmount(BigDecimal.ZERO);
        order.setFinalAmount(totalAmount);
        prepareInitialStatuses(order);

        orderRepository.save(order);

        // Lưu lịch sử sử dụng voucher giảm giá đơn
        if (voucher != null) {
            createVoucherUsage(order, voucher, customer, voucherDiscount);
        }
        // Lưu lịch sử sử dụng voucher Freeship
        if (freeShipVoucher != null) {
            createVoucherUsage(order, freeShipVoucher, customer, shippingDiscount);
        }

        PaymentTransaction paymentTransaction = createInitialPaymentTransaction(order);

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        String paymentUrl = null;
        if (isGateway(order.getPaymentMethod())) {
            PaymentGatewayClient client = findGatewayClient(order.getPaymentMethod().name());
            PaymentInitResponse initResponse = client.createPaymentUrl(order, paymentTransaction);
            paymentUrl = initResponse.getPaymentUrl();
        }

        if (PaymentMethod.BANK_TRANSFER.equals(order.getPaymentMethod())) {
            telegramNotificationService.sendMessage(
                    "🆕 <b>Đơn hàng mới chờ chuyển khoản</b>\n"
                            + "Mã đơn: <b>" + order.getOrderCode() + "</b>\n"
                            + "Lookup: <b>" + order.getLookupCode() + "</b>\n"
                            + "Tổng tiền: <b>" + order.getTotalAmount() + " VND</b>\n"
                            + "Khách: <b>" + safe(order.getOrdererName()) + "</b>\n"
                            + "SĐT: <b>" + safe(order.getOrdererPhone()) + "</b>\n"
                            + "Nội dung CK: <b>" + safe(paymentTransaction.getIdempotencyKey()) + "</b>"
            );
        }

        String paymentCode = null;
        String transferContent = null;
        String qrImageUrl = null;
        String bankCode = null;
        String bankName = null;
        String bankAccountNo = null;
        String bankAccountName = null;

        if (PaymentMethod.BANK_TRANSFER.equals(order.getPaymentMethod()) && sepayService.isEnabled()) {
            paymentCode = paymentTransaction.getIdempotencyKey();
            transferContent = sepayService.buildTransferContent(order.getId());
            qrImageUrl = sepayService.buildQrImageUrl(order.getTotalAmount(), transferContent);
            bankCode = sepayService.getBankCode();
            bankName = sepayService.getBankName();
            bankAccountNo = sepayService.getBankAccountNo();
            bankAccountName = sepayService.getAccountName();
        }

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .lookupCode(order.getLookupCode())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingStatus(order.getShippingStatus())
                .paymentMethod(order.getPaymentMethod())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .finalAmount(order.getFinalAmount())
                .paymentUrl(paymentUrl)
                .paymentCode(paymentCode)
                .bankCode(bankCode)
                .bankName(bankName)
                .bankAccountNo(bankAccountNo)
                .bankAccountName(bankAccountName)
                .transferContent(transferContent)
                .qrImageUrl(qrImageUrl)
                .message("Tạo đơn hàng thành công")
                .build();
    }

    private Cart resolveCart(Long cartId, Long customerId, String sessionKey) {
        if (cartId != null) {
            return cartRepository.findByIdAndDeletedFalse(cartId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng: " + cartId));
        }
        if (customerId != null) {
            return cartRepository.findByCustomer_IdAndStatusAndDeletedFalse(customerId, CartStatus.ACTIVE)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cart active của khách hàng"));
        }
        if (sessionKey != null && !sessionKey.isBlank()) {
            return cartRepository.findBySessionKeyAndStatusAndDeletedFalse(sessionKey, CartStatus.ACTIVE)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cart active của guest"));
        }
        throw new RuntimeException("Thiếu cartId hoặc customerId hoặc sessionKey");
    }

    private Customer resolveCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + customerId));
    }

    private void validateCart(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }
    }

    private void fillPreviewOrderContact(Order order, Cart cart, Customer customer) {
        order.setCustomer(customer);
        order.setGuestOrder(customer == null);

        if (customer != null) {
            order.setOrdererName(customer.getTen());
            order.setOrdererEmail(customer.getEmail());
            order.setOrdererPhone(customer.getPhone());
        } else {
            order.setOrdererEmail(cart.getGuestEmail());
            order.setOrdererPhone(cart.getGuestPhone());
        }
    }

    private void fillOrderHeader(Order order, CheckoutRequest request, Cart cart, Customer customer) {
        order.setCart(cart);
        order.setCustomer(customer);
        order.setGuestOrder(customer == null);
        order.setChannel(SalesChannel.ONLINE);
        order.setOrderCode("ORD" + System.currentTimeMillis());
        order.setLookupCode("LOOKUP" + System.currentTimeMillis());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCurrencyCode("VND");
        order.setReturnStatus(ReturnStatus.NONE);
        order.setShippingStatus(ShippingStatus.PENDING);
        order.setOrdererName(request.getOrdererName());
        order.setOrdererEmail(request.getOrdererEmail());
        order.setOrdererPhone(request.getOrdererPhone());

        if (customer != null) {
            if (order.getOrdererName() == null || order.getOrdererName().isBlank()) {
                order.setOrdererName(customer.getTen());
            }
            if (order.getOrdererEmail() == null || order.getOrdererEmail().isBlank()) {
                order.setOrdererEmail(customer.getEmail());
            }
            if (order.getOrdererPhone() == null || order.getOrdererPhone().isBlank()) {
                order.setOrdererPhone(customer.getPhone());
            }
        } else {
            if (order.getOrdererEmail() == null || order.getOrdererEmail().isBlank()) {
                order.setOrdererEmail(cart.getGuestEmail());
            }
            if (order.getOrdererPhone() == null || order.getOrdererPhone().isBlank()) {
                order.setOrdererPhone(cart.getGuestPhone());
            }
        }

        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setAddressLabel(request.getAddressLabel());
        order.setShippingProvince(request.getShippingProvince());
        order.setShippingDistrict(request.getShippingDistrict());
        order.setShippingWard(request.getShippingWard());
        order.setShippingDetailAddress(request.getShippingDetailAddress());
        order.setNote(request.getNote());
        order.rebuildShippingAddressLine();
    }

    private void snapshotItem(OrderItem item, ProductVariant variant) {
        item.setProductIdSnapshot(variant.getProduct() != null ? variant.getProduct().getId() : null);
        item.setVariantIdSnapshot(variant.getId());
        item.setSkuSnapshot(variant.getSku());
        item.setProductNameSnapshot(variant.getProduct() != null ? variant.getProduct().getName() : null);
        item.setColorSnapshot(variant.getColor() != null ? variant.getColor().getName() : null);
        item.setSizeSnapshot(variant.getSize() != null ? variant.getSize().getName() : null);
        item.setMaterialSnapshot(
                variant.getProduct() != null && variant.getProduct().getMaterial() != null
                        ? variant.getProduct().getMaterial().getName()
                        : null
        );
        item.setSoleSnapshot(
                variant.getProduct() != null && variant.getProduct().getSole() != null
                        ? variant.getProduct().getSole().getName()
                        : null
        );
        item.setImageUrlSnapshot(variant.getImageUrl());
    }

    private void validateStock(ProductVariant variant, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        int available = getAvailableQuantity(variant);
        if (quantity > available) {
            throw new OutOfStockException(
                    "Không đủ tồn kho cho SKU " + variant.getSku()
                            + ". Khả dụng: " + available
                            + ", cần: " + quantity
            );
        }
    }

    private Voucher validateAndResolveVoucher(String voucherCode, Customer customer, Order order, BigDecimal subtotal) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(voucherCode.trim())
                .orElseThrow(() -> new VoucherInvalidException("Voucher không tồn tại"));

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(voucher.getDeleted())) {
            throw new VoucherInvalidException("Voucher đã bị xóa");
        }
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            throw new VoucherInvalidException("Voucher không ở trạng thái ACTIVE");
        }
        if (voucher.getStartDate() != null && voucher.getStartDate().isAfter(now)) {
            throw new VoucherInvalidException("Voucher chưa đến thời gian sử dụng");
        }
        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now)) {
            throw new VoucherInvalidException("Voucher đã hết hạn");
        }
        if (voucher.getQuantity() != null && voucher.getUsedCount() != null && voucher.getUsedCount() >= voucher.getQuantity()) {
            throw new VoucherInvalidException("Voucher đã hết lượt sử dụng");
        }

        BigDecimal minOrder = voucher.getMinOrderValue() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(voucher.getMinOrderValue());

        if (subtotal.compareTo(minOrder) < 0) {
            throw new VoucherInvalidException("Đơn hàng chưa đạt giá trị tối thiểu để áp voucher");
        }

        validateVoucherCustomerScope(voucher, customer);

        if (customer != null) {
            if (voucherUsageRepository.existsByVoucher_IdAndCustomer_Id(voucher.getId(), customer.getId())) {
                throw new VoucherInvalidException("Khách hàng đã sử dụng voucher này trước đó");
            }
        } else {
            String email = order.getOrdererEmail();
            if (email != null && voucherUsageRepository.existsByVoucher_IdAndGuestEmail(voucher.getId(), email)) {
                throw new VoucherInvalidException("Guest đã sử dụng voucher này trước đó");
            }
        }

        return voucher;
    }

    private BigDecimal calculateVoucherDiscount(Voucher voucher, BigDecimal subtotal) {
        if (voucher == null) return BigDecimal.ZERO;

        BigDecimal value = voucher.getValue() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(voucher.getValue());

        BigDecimal maxDiscount = voucher.getMaxDiscount() == null
                ? null
                : BigDecimal.valueOf(voucher.getMaxDiscount());

        BigDecimal discount = "PERCENT".equalsIgnoreCase(voucher.getType())
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : value;

        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount.max(BigDecimal.ZERO);
    }

    // 🔥 FIX: HÀM TÍNH SHIP ĐÃ KIỂM TRA ĐỊA CHỈ
    private BigDecimal calculateShippingFee(String province, BigDecimal subtotal) {
        // 1. Khách chưa chọn Tỉnh/Thành -> Không tính phí ship
        if (province == null || province.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 2. Mua trên 10 triệu -> Tự động Freeship
        if (subtotal.compareTo(new BigDecimal("10000000")) >= 0) {
            return BigDecimal.ZERO;
        }

        String p = province.toLowerCase();

        // 3. Cùng tỉnh (Giả sử shop ở Hà Nội) -> Phí rẻ nhất
        if (p.contains("hà nội")) {
            return new BigDecimal("20000");
        }

        // 4. Các tỉnh Miền Bắc (Gần Hà Nội) -> Phí 30k
        List<String> mienBac = Arrays.asList(
                "hà giang", "cao bằng", "bắc kạn", "tuyên quang", "lào cai", "điện biên", "lai châu", "sơn la", "yên bái", "hoà bình",
                "thái nguyên", "lạng sơn", "quảng ninh", "bắc giang", "phú thọ", "vĩnh phúc", "bắc ninh", "hải dương", "hải phòng", "hưng yên", "thái bình", "hà nam", "nam định", "ninh bình"
        );
        boolean isMienBac = mienBac.stream().anyMatch(p::contains);
        if (isMienBac) {
            return new BigDecimal("30000");
        }

        // 5. Các tỉnh Miền Trung & Tây Nguyên -> Phí 40k
        List<String> mienTrung = Arrays.asList(
                "thanh hóa", "nghệ an", "hà tĩnh", "quảng bình", "quảng trị", "thừa thiên huế",
                "đà nẵng", "quảng nam", "quảng ngãi", "bình định", "phú yên", "khánh hòa", "ninh thuận", "bình thuận",
                "kon tum", "gia lai", "đắk lắk", "đắk nông", "lâm đồng"
        );
        boolean isMienTrung = mienTrung.stream().anyMatch(p::contains);
        if (isMienTrung) {
            return new BigDecimal("40000");
        }

        // 6. Các tỉnh Miền Nam (Bao gồm TP.HCM, Cần Thơ và phần còn lại) -> Phí 50k
        return new BigDecimal("50000");
    }

    private void prepareInitialStatuses(Order order) {
        order.setOrderStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentMethod.COD.equals(order.getPaymentMethod())
                ? PaymentStatus.UNPAID
                : PaymentStatus.PENDING);
    }

    private PaymentTransaction createInitialPaymentTransaction(Order order) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrder(order);
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setPaymentMethod(order.getPaymentMethod());
        tx.setRequestAmount(order.getTotalAmount());
        tx.setCurrencyCode(order.getCurrencyCode());

        if (PaymentMethod.COD.equals(order.getPaymentMethod())) {
            tx.setStatus(TransactionStatus.PENDING);
            tx.setProvider("COD");
        } else if (PaymentMethod.BANK_TRANSFER.equals(order.getPaymentMethod())) {
            tx.setStatus(TransactionStatus.PENDING);
            tx.setProvider("SEPAY");
            tx.setIdempotencyKey(sepayService.buildPaymentCode(order.getId()));
            tx.setProviderMessage("Cho thanh toan don " + order.getOrderCode());
        } else {
            tx.setStatus(TransactionStatus.PENDING);
            tx.setProvider(order.getPaymentMethod().name());
            tx.setIdempotencyKey(UUID.randomUUID().toString());
        }

        return paymentTransactionRepository.save(tx);
    }

    private void createVoucherUsage(Order order, Voucher voucher, Customer customer, BigDecimal discountAmount) {
        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setCustomer(customer);
        usage.setOrderId(order.getId());
        usage.setGuestEmail(customer == null ? order.getOrdererEmail() : null);
        usage.setGuestPhone(customer == null ? order.getOrdererPhone() : null);
        usage.setUsageType(customer != null ? "CUSTOMER" : "GUEST");
        usage.setVoucherCodeSnapshot(voucher.getCode());
        usage.setDiscountAmount(discountAmount.doubleValue());
        usage.setUsedAt(LocalDateTime.now());
        voucherUsageRepository.save(usage);

        voucher.setUsedCount((voucher.getUsedCount() == null ? 0 : voucher.getUsedCount()) + 1);
        voucherRepository.save(voucher);
    }

    private boolean isGateway(PaymentMethod method) {
        return PaymentMethod.VNPAY.equals(method) || PaymentMethod.MOMO.equals(method);
    }

    private PaymentGatewayClient findGatewayClient(String provider) {
        return paymentGatewayClients.stream()
                .filter(c -> c.supports(provider))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gateway client cho provider: " + provider));
    }

    private BigDecimal resolveBaseUnitPrice(ProductVariant variant) {
        if (variant.getSalePrice() != null && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getSalePrice();
        }
        if (variant.getPrice() != null && variant.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getPrice();
        }
        throw new RuntimeException("Biến thể " + variant.getId() + " chưa có giá hợp lệ");
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private PriceSnapshot resolveCheckoutPrice(ProductVariant variant, Integer quantity) {
        try {
            PriceResultDTO priceResult = promotionPricingService.calculateFinalPrice(variant.getId(), quantity);

            BigDecimal promotionDiscount = nz(priceResult.getDiscountAmount());
            BigDecimal finalLineTotal = nz(priceResult.getFinalPrice());

            return new PriceSnapshot(finalLineTotal, promotionDiscount);
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Không tìm thấy giá")) {
                BigDecimal unitPrice = resolveBaseUnitPrice(variant);
                BigDecimal finalLineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity));

                return new PriceSnapshot(finalLineTotal, BigDecimal.ZERO);
            }
            throw ex;
        }
    }

    private static class PriceSnapshot {
        private final BigDecimal finalLineTotal;
        private final BigDecimal promotionDiscount;

        public PriceSnapshot(BigDecimal finalLineTotal, BigDecimal promotionDiscount) {
            this.finalLineTotal = finalLineTotal;
            this.promotionDiscount = promotionDiscount;
        }

        public BigDecimal getFinalLineTotal() {
            return finalLineTotal;
        }

        public BigDecimal getPromotionDiscount() {
            return promotionDiscount;
        }
    }
}