package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.AddToCartRequest;
import com.sneakershop.backend.dto.order.CartItemResponse;
import com.sneakershop.backend.dto.order.CartResponse;
import com.sneakershop.backend.dto.order.UpdateCartItemQuantityRequest;
import com.sneakershop.backend.dto.order.UpdateCartItemSelectionRequest;
import com.sneakershop.backend.dto.pricing.PriceResultDTO;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.Cart;
import com.sneakershop.backend.entity.order.CartItem;
import com.sneakershop.backend.entity.order.enums.CartStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.login.UserRepository;
import com.sneakershop.backend.repository.order.CartItemRepository;
import com.sneakershop.backend.repository.order.CartRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.service.pricing.ProductPricingPromotionService;
import com.sneakershop.backend.service.ValidationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ProductPricingPromotionService promotionPricingService;

    @Transactional
    public CartResponse getCurrentCart(String principalName, String sessionKey) {
        Cart cart = getOrCreateActiveCart(principalName, sessionKey);
        return mapCart(cart);
    }
    private int getAvailableStock(ProductVariant variant) {
        if (variant == null) return 0;
        return Math.max(variant.getStock() - Math.max(variant.getReserved_quantity(), 0), 0);
    }

    @Transactional
    public CartResponse addToCart(String principalName, String sessionKey, AddToCartRequest request) {
        if (request.getVariantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "variantId không được để trống");
        }

        Integer quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm phải là số nguyên dương.");
        }

        Cart cart = getOrCreateActiveCart(principalName, sessionKey);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể sản phẩm"));

        if (getAvailableStock(variant) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm đã hết hàng");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;

            validateStock(variant, newQuantity);

            existingItem.setQuantity(newQuantity);
            existingItem.setSelected(true);
            cartItemRepository.save(existingItem);
        } else {
            validateStock(variant, quantity);

            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(quantity);
            newItem.setSelected(true);

            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        cartRepository.save(cart);

        Cart latestCart = cartRepository.findByIdAndDeletedFalse(cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ hàng"));

        return mapCart(latestCart);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(
            String principalName,
            String sessionKey,
            Long itemId,
            UpdateCartItemQuantityRequest request
    ) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm phải là số nguyên dương.");
        }

        Cart cart = getOrCreateActiveCart(principalName, sessionKey);
        CartItem item = getOwnedCartItem(cart.getId(), itemId);

        ProductVariant variant = productVariantRepository.findById(item.getVariant().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể sản phẩm"));

        validateStock(variant, request.getQuantity());

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart latestCart = cartRepository.findByIdAndDeletedFalse(cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ hàng"));

        return mapCart(latestCart);
    }

    @Transactional
    public CartResponse updateCartItemSelection(
            String principalName,
            String sessionKey,
            Long itemId,
            UpdateCartItemSelectionRequest request
    ) {
        if (request.getSelected() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "selected không được để trống");
        }

        Cart cart = getOrCreateActiveCart(principalName, sessionKey);
        CartItem item = getOwnedCartItem(cart.getId(), itemId);

        item.setSelected(request.getSelected());
        cartItemRepository.save(item);

        Cart latestCart = cartRepository.findByIdAndDeletedFalse(cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ hàng"));

        return mapCart(latestCart);
    }

    @Transactional
    public CartResponse removeCartItem(String principalName, String sessionKey, Long itemId) {
        Cart cart = getOrCreateActiveCart(principalName, sessionKey);
        CartItem item = getOwnedCartItem(cart.getId(), itemId);

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        cartRepository.save(cart);

        Cart latestCart = cartRepository.findByIdAndDeletedFalse(cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ hàng"));

        return mapCart(latestCart);
    }

    @Transactional
    public CartResponse clearCart(String principalName, String sessionKey) {
        Cart cart = getOrCreateActiveCart(principalName, sessionKey);

        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            cart.getItems().clear();
        }

        cartRepository.save(cart);
        return mapCart(cart);
    }

    private Cart getOrCreateActiveCart(String principalName, String sessionKey) {
        User user = resolveCurrentUser(principalName);

        if (user != null) {
            Customer customer = customerRepository.findByUserId(user.getId())
                    .orElseGet(() -> createCustomerForUser(user));

            return cartRepository.findByCustomer_IdAndStatusAndDeletedFalse(customer.getId(), CartStatus.ACTIVE)
                    .orElseGet(() -> createCartForCustomer(customer));
        }

        if (sessionKey == null || sessionKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu X-Cart-Session-Key cho guest");
        }

        return cartRepository.findBySessionKeyAndStatusAndDeletedFalse(sessionKey, CartStatus.ACTIVE)
                .orElseGet(() -> createCartForGuest(sessionKey));
    }

    private User resolveCurrentUser(String principalName) {
        if (principalName == null || principalName.trim().isEmpty()) {
            return null;
        }

        return userRepository.findByUsername(principalName)
                .or(() -> userRepository.findByEmail(principalName))
                .orElse(null);
    }

    private Customer createCustomerForUser(User user) {
        Customer customer = new Customer();
        customer.setTen(
                user.getFullName() != null && !user.getFullName().isBlank()
                        ? user.getFullName()
                        : user.getEmail()
        );
        customer.setEmail(user.getEmail());
        customer.setStatus("ACTIVE");
        customer.setLoaiKhach("BRONZE");
        customer.setDiemTichLuy(0);
        customer.setUser(user);
        return customerRepository.save(customer);
    }

    private Cart createCartForCustomer(Customer customer) {
        Cart cart = new Cart();
        cart.setCustomer(customer);
        cart.setChannel(SalesChannel.ONLINE);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private Cart createCartForGuest(String sessionKey) {
        Cart cart = new Cart();
        cart.setSessionKey(sessionKey);
        cart.setChannel(SalesChannel.ONLINE);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private CartItem getOwnedCartItem(Long cartId, Long itemId) {
        return cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ hàng"));
    }

    private void validateStock(ProductVariant variant, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm phải là số nguyên dương.");
        }

        int availableStock = getAvailableStock(variant);

        if (availableStock < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng mua vượt quá tồn kho hiện có. Tồn kho còn lại: " + availableStock + "."
            );
        }
    }

    private CartResponse mapCart(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setCustomerId(cart.getCustomer() != null ? cart.getCustomer().getId() : null);
        response.setSessionKey(cart.getSessionKey());

        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::mapCartItem)
                .collect(Collectors.toList());

        response.setItems(items);

        int totalItems = items.stream()
                .map(CartItemResponse::getQuantity)
                .filter(q -> q != null && q > 0)
                .reduce(0, Integer::sum);

        int selectedItemCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getSelected()))
                .map(CartItemResponse::getQuantity)
                .filter(q -> q != null && q > 0)
                .reduce(0, Integer::sum);

        BigDecimal subtotal = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getSelected()))
                .map(CartItemResponse::getLineTotal)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalItems(totalItems);
        response.setSelectedItemCount(selectedItemCount);
        response.setSubtotal(subtotal);

        return response;
    }

    private CartItemResponse mapCartItem(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();

        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

        BigDecimal originalUnitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
        BigDecimal saleUnitPrice = null;
        BigDecimal unitPrice = originalUnitPrice;
        BigDecimal lineTotal = originalUnitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountAmount = BigDecimal.ZERO;
        String promotionName = null;
        boolean onSale = false;

        if (quantity > 0) {
            PriceResultDTO priceResult = promotionPricingService.calculateFinalPrice(variant.getId(), quantity);

            if (priceResult != null) {
                BigDecimal originalTotal = safe(priceResult.getOriginalPrice());
                BigDecimal finalTotal = safe(priceResult.getFinalPrice());
                BigDecimal totalDiscount = safe(priceResult.getDiscountAmount());

                lineTotal = finalTotal;
                discountAmount = totalDiscount;
                promotionName = priceResult.getPromotionName();
                onSale = priceResult.isOnSale() && totalDiscount.compareTo(BigDecimal.ZERO) > 0;

                if (quantity > 0) {
                    originalUnitPrice = divideSafe(originalTotal, quantity);
                    unitPrice = divideSafe(finalTotal, quantity);
                }

                if (onSale && unitPrice.compareTo(originalUnitPrice) < 0) {
                    saleUnitPrice = unitPrice;
                } else {
                    saleUnitPrice = null;
                    unitPrice = originalUnitPrice;
                }
            }
        }

        CartItemResponse dto = new CartItemResponse();
        dto.setItemId(item.getId());
        dto.setVariantId(variant.getId());
        dto.setProductId(product != null ? product.getId() : null);
        dto.setProductName(product != null ? product.getName() : null);
        dto.setSku(variant.getSku());
        dto.setImageUrl(
                variant.getImageUrl() != null && !variant.getImageUrl().isBlank()
                        ? variant.getImageUrl()
                        : (product != null ? product.getThumbnail() : null)
        );
        dto.setColor(variant.getColor() != null ? variant.getColor().getName() : null);
        dto.setSize(variant.getSize() != null ? variant.getSize().getName() : null);
        dto.setQuantity(quantity);
        dto.setStock(getAvailableStock(variant));
        dto.setSelected(Boolean.TRUE.equals(item.getSelected()));

        dto.setOriginalUnitPrice(originalUnitPrice);
        dto.setSaleUnitPrice(saleUnitPrice);
        dto.setUnitPrice(unitPrice);
        dto.setDiscountAmount(discountAmount);
        dto.setLineTotal(lineTotal);
        dto.setPromotionName(promotionName);
        dto.setOnSale(onSale);

        return dto;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal divideSafe(BigDecimal total, int quantity) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return safe(total).divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }
}