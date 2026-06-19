package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.CreateOrderRequest;
import com.rajlaxmi.jewellers.dto.response.*;
import com.rajlaxmi.jewellers.entity.*;
import com.rajlaxmi.jewellers.enums.OrderStatus;
import com.rajlaxmi.jewellers.enums.PaymentStatus;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.*;
import com.rajlaxmi.jewellers.service.GoldPriceService;
import com.rajlaxmi.jewellers.service.OrderService;
import com.rajlaxmi.jewellers.service.PaymentService;
import com.rajlaxmi.jewellers.util.PricingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderServiceImpl
 *
 * ORDER PLACEMENT FLOW:
 *  1. Load cart items for user → validate all items are in stock
 *  2. Fetch current gold price (snapshot for this order)
 *  3. Calculate pricing for each item via PricingEngine
 *  4. Apply coupon discount if provided
 *  5. Load and snapshot shipping address
 *  6. Create Order + OrderItems within a single transaction
 *  7. Decrement inventory (reserve stock)
 *  8. Clear cart
 *  9. Return OrderResponse
 *
 * TRANSACTION:
 *  The entire flow is @Transactional — if inventory decrement fails
 *  (e.g. race condition, stock gone), the whole order rolls back.
 *  This prevents overselling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final GoldPriceService goldPriceService;
    private final PricingEngine pricingEngine;
    private final PaymentService paymentService;

    // ── Place Order ───────────────────────────────────────────

    @Override
    public ApiResponse<OrderResponse> placeOrder(Long userId, CreateOrderRequest request) {
        // 1. Validate cart is not empty
        List<Cart> cartItems = cartRepository.findByUserIdWithProducts(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException("Your cart is empty. Please add items before placing an order.");
        }

        // 2. Validate shipping address belongs to user
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessException("Invalid shipping address.");
        }

        // 3. Validate serviceable pincode (Bihar and Jharkhand only)
        validateServiceablePincode(address.getState());

        // 4. Fetch gold price snapshot
        GoldPrice goldPrice = goldPriceService.getCurrentGoldPriceEntity();

        // 5. Build order items and validate stock
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        for (Cart cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Validate in stock
            if (product.getInventory() == null || product.getInventory().getAvailableQuantity() < cartItem.getQuantity()) {
                throw new BusinessException("'" + product.getName() + "' does not have enough stock. " +
                        "Available: " + (product.getInventory() != null ?
                        product.getInventory().getAvailableQuantity() : 0));
            }

            // Calculate price via PricingEngine
            PricingEngine.PriceBreakdown price = pricingEngine.calculatePrice(product, goldPrice);

            // Determine gold rate used for this item
            BigDecimal goldRateUsed = product.getGoldPurity() != null
                    ? switch (product.getGoldPurity()) {
                        case GOLD_18K -> goldPrice.getRate18K();
                        case GOLD_22K -> goldPrice.getRate22K();
                        case GOLD_24K -> goldPrice.getRate24K();
                    } : goldPrice.getRate22K();

            // Get primary image
            String primaryImage = product.getImages().stream()
                    .filter(ProductImage::isPrimary).findFirst()
                    .or(() -> product.getImages().stream().findFirst())
                    .map(ProductImage::getImageUrl).orElse(null);

            BigDecimal itemTotal = price.getFinalPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            BigDecimal itemGst = price.getGstAmount().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productSlug(product.getSlug())
                    .primaryImageUrl(primaryImage)
                    .metalType(product.getMetalType())
                    .goldPurity(product.getGoldPurity())
                    .weightGrams(product.getWeightGrams())
                    .goldRateUsed(goldRateUsed)
                    .baseMetalValue(price.getBaseMetalValue())
                    .makingCharges(price.getMakingCharges())
                    .stoneCharges(price.getStoneCharges())
                    .gstPercentage(price.getGstPercentage())
                    .gstAmount(itemGst)
                    .unitPrice(price.getFinalPrice())
                    .quantity(cartItem.getQuantity())
                    .totalPrice(itemTotal)
                    .isBisHallmarked(product.isBisHallmarked())
                    .bisHallmarkNumber(product.getBisHallmarkNumber())
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(itemTotal);
            totalGst = totalGst.add(itemGst);
        }

        // 6. Apply coupon discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCouponCode = null;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCouponCode())
                    .orElseThrow(() -> new BusinessException("Coupon code '" + request.getCouponCode() + "' is not valid."));

            if (!coupon.isValid()) {
                throw new BusinessException("Coupon '" + request.getCouponCode() + "' has expired or reached its usage limit.");
            }

            discountAmount = coupon.calculateDiscount(subtotal);
            if (discountAmount.compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessException("Minimum order amount for this coupon is ₹" + coupon.getMinimumOrderAmount());
            }

            appliedCouponCode = coupon.getCode();
            couponRepository.incrementUsedCount(coupon.getId());
        }

        BigDecimal grandTotal = subtotal.subtract(discountAmount);

        // 7. Generate order number
        String orderNumber = generateOrderNumber();

        // 8. Create Order
        User user = userRepository.getReferenceById(userId);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingFullName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingStreet(address.getStreetAddress() +
                        (address.getLandmark() != null ? ", " + address.getLandmark() : ""))
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingPincode(address.getPincode())
                .subtotal(subtotal)
                .totalGst(totalGst)
                .discountAmount(discountAmount)
                .shippingCharge(BigDecimal.ZERO)
                .grandTotal(grandTotal)
                .goldRate22KAtOrder(goldPrice.getRate22K())
                .goldRate24KAtOrder(goldPrice.getRate24K())
                .couponCode(appliedCouponCode)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .customerNote(request.getCustomerNote())
                .build();

        order = orderRepository.save(order);

        // 9. Save order items with order reference
        Order finalOrder = order;
        orderItems.forEach(item -> {
            item.setOrder(finalOrder);
            finalOrder.getItems().add(item);
        });
        orderRepository.save(order);

        // 10. Decrement inventory for each item
        for (Cart cartItem : cartItems) {
            int decremented = inventoryRepository.decrementStock(
                    cartItem.getProduct().getId(), cartItem.getQuantity());

            if (decremented == 0) {
                // This should not happen due to earlier check, but safety net
                throw new BusinessException("Stock update failed for: " + cartItem.getProduct().getName() +
                        ". Order cannot be completed.");
            }
        }

        // 11. Create payment record for UPI/COD follow-up endpoints
        paymentService.createPaymentForOrder(order);

        // 12. Clear cart
        cartRepository.clearCartByUserId(userId);

        log.info("Order placed: {} by user: {} | Total: ₹{}", orderNumber, userId, grandTotal);

        return ApiResponse.success(
                "Order placed successfully! Order #" + orderNumber +
                " confirmed. We'll notify you via WhatsApp on " +
                address.getPhone() + ".",
                toOrderResponse(order));
    }

    // ── Read Operations ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Users can only see their own orders; admins bypass this in controller
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Access denied.");
        }
        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        return toOrderResponse(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(Long userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.from(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable),
                this::toOrderResponse);
    }

    // ── Cancel Order ──────────────────────────────────────────

    @Override
    public ApiResponse<OrderResponse> cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Access denied.");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Cannot cancel an order that has already been shipped or delivered.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled.");
        }

        // Restore inventory
        order.getItems().forEach(item ->
                inventoryRepository.incrementStock(item.getProductId(), item.getQuantity()));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order cancelled: {} by user: {}", order.getOrderNumber(), userId);
        return ApiResponse.success("Order #" + order.getOrderNumber() + " has been cancelled.", toOrderResponse(order));
    }

    // ── Admin Operations ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.from(orderRepository.findAllByOrderByCreatedAtDesc(pageable), this::toOrderResponse);
    }

    @Override
    public ApiResponse<OrderResponse> updateOrderStatus(Long orderId, OrderStatus status, String adminNote) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(status);
        if (adminNote != null) order.setAdminNote(adminNote);

        // Set status-specific timestamps
        switch (status) {
            case CONFIRMED -> {
                order.setConfirmedAt(LocalDateTime.now());
                order.setPaymentStatus(PaymentStatus.SUCCESS);
            }
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                order.getItems().forEach(item ->
                        inventoryRepository.incrementStock(item.getProductId(), item.getQuantity()));
            }
            default -> {}
        }

        orderRepository.save(order);
        return ApiResponse.success("Order status updated to " + status.name(), toOrderResponse(order));
    }

    @Override
    public ApiResponse<OrderResponse> updatePaymentStatus(Long orderId, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setPaymentTransactionId(transactionId);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(LocalDateTime.now());
        }
        orderRepository.save(order);
        return ApiResponse.success("Payment verified for order #" + order.getOrderNumber(), toOrderResponse(order));
    }

    // ── Coupon Validation ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CouponResponse> validateCoupon(String code, BigDecimal orderTotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException("Coupon '" + code + "' not found."));

        if (!coupon.isValid()) {
            throw new BusinessException("This coupon has expired or reached its usage limit.");
        }

        BigDecimal discount = coupon.calculateDiscount(orderTotal);

        return ApiResponse.success("Coupon applied!", CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .isValid(true)
                .applicableDiscountAmount(discount)
                .build());
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Generates order number: RLJ-YYYY-NNNNNN
     * e.g. RLJ-2025-000042
     */
    private String generateOrderNumber() {
        long count = orderRepository.countAllOrders() + 1;
        return String.format("RLJ-%d-%06d", Year.now().getValue(), count);
    }

    /**
     * Validates that delivery is available to the given state.
     * Currently serves Bihar and Jharkhand only.
     */
    private void validateServiceablePincode(String state) {
        List<String> serviceableStates = List.of("Bihar", "Jharkhand");
        if (!serviceableStates.stream().anyMatch(s -> s.equalsIgnoreCase(state))) {
            throw new BusinessException(
                    "Sorry, we currently deliver only to Bihar and Jharkhand. " +
                    "Your address state '" + state + "' is not serviceable yet.");
        }
    }

    private OrderResponse toOrderResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .items(o.getItems().stream().map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .productSlug(item.getProductSlug())
                        .primaryImageUrl(item.getPrimaryImageUrl())
                        .metalType(item.getMetalType())
                        .goldPurity(item.getGoldPurity())
                        .weightGrams(item.getWeightGrams())
                        .goldRateUsed(item.getGoldRateUsed())
                        .baseMetalValue(item.getBaseMetalValue())
                        .makingCharges(item.getMakingCharges())
                        .stoneCharges(item.getStoneCharges())
                        .gstPercentage(item.getGstPercentage())
                        .gstAmount(item.getGstAmount())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getTotalPrice())
                        .isBisHallmarked(item.isBisHallmarked())
                        .bisHallmarkNumber(item.getBisHallmarkNumber())
                        .build()).toList())
                .shippingFullName(o.getShippingFullName())
                .shippingPhone(o.getShippingPhone())
                .shippingStreet(o.getShippingStreet())
                .shippingCity(o.getShippingCity())
                .shippingState(o.getShippingState())
                .shippingPincode(o.getShippingPincode())
                .subtotal(o.getSubtotal())
                .totalGst(o.getTotalGst())
                .discountAmount(o.getDiscountAmount())
                .shippingCharge(o.getShippingCharge())
                .grandTotal(o.getGrandTotal())
                .goldRate22KAtOrder(o.getGoldRate22KAtOrder())
                .couponCode(o.getCouponCode())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .paymentTransactionId(o.getPaymentTransactionId())
                .trackingNumber(o.getTrackingNumber())
                .shippingPartner(o.getShippingPartner())
                .customerNote(o.getCustomerNote())
                .createdAt(o.getCreatedAt())
                .confirmedAt(o.getConfirmedAt())
                .shippedAt(o.getShippedAt())
                .deliveredAt(o.getDeliveredAt())
                .build();
    }
}
