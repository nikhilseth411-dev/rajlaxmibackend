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
    private final PaymentRepository paymentRepository;
    private final GoldPriceService goldPriceService;
    private final PricingEngine pricingEngine;
    private final PaymentService paymentService;

    // ── Place Order ───────────────────────────────────────────

    @Override
    public ApiResponse<OrderResponse> placeOrder(Long userId, CreateOrderRequest request) {
        if (request.getPaymentMethod() == null) {
            throw new BusinessException("Please select a payment method.");
        }

        List<Cart> cartItems = cartRepository.findByUserIdWithProducts(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException("Your cart is empty. Please add items before placing an order.");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new BusinessException("Invalid shipping address.");
        }

        validateServiceablePincode(address.getState());

        GoldPrice goldPrice = goldPriceService.getCurrentGoldPriceEntity();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        for (Cart cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getInventory() == null ||
                    product.getInventory().getAvailableQuantity() < cartItem.getQuantity()) {
                throw new BusinessException("'" + product.getName() + "' does not have enough stock. Available: " +
                        (product.getInventory() != null ? product.getInventory().getAvailableQuantity() : 0));
            }

            PricingEngine.PriceBreakdown price = pricingEngine.calculatePrice(product, goldPrice);

            BigDecimal goldRateUsed = product.getGoldPurity() != null
                    ? switch (product.getGoldPurity()) {
                case GOLD_18K -> goldPrice.getRate18K();
                case GOLD_22K -> goldPrice.getRate22K();
                case GOLD_24K -> goldPrice.getRate24K();
            }
                    : goldPrice.getRate22K();

            String primaryImage = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .or(() -> product.getImages().stream().findFirst())
                    .map(ProductImage::getImageUrl)
                    .orElse(null);

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
        String orderNumber = generateOrderNumber();

        User user = userRepository.getReferenceById(userId);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingFullName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingStreet(address.getStreetAddress() +
                        (address.getLandmark() != null && !address.getLandmark().isBlank()
                                ? ", " + address.getLandmark()
                                : ""))
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

        Order finalOrder = order;
        orderItems.forEach(item -> {
            item.setOrder(finalOrder);
            finalOrder.getItems().add(item);
        });

        orderRepository.save(order);

        for (Cart cartItem : cartItems) {
            int decremented = inventoryRepository.decrementStock(
                    cartItem.getProduct().getId(),
                    cartItem.getQuantity()
            );

            if (decremented == 0) {
                throw new BusinessException("Stock update failed for: " +
                        cartItem.getProduct().getName() + ". Order cannot be completed.");
            }
        }

        paymentService.createPaymentForOrder(order);

        cartRepository.clearCartByUserId(userId);

        log.info("Order placed: {} by user: {} | Total: ₹{}", orderNumber, userId, grandTotal);

        return ApiResponse.success(
                "Order placed successfully! Order #" + orderNumber +
                        " created. Please complete payment to confirm your order.",
                toOrderResponse(order)
        );
    }

    // ── Read Operations ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

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
                this::toOrderResponse
        );
    }

    // ── Cancel Order ──────────────────────────────────────────

    @Override
    public ApiResponse<OrderResponse> cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Access denied.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled.");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Cannot cancel an order that has already been shipped or delivered.");
        }

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("Paid orders cannot be cancelled directly. Please contact Raj Laxmi Jewellers support.");
        }

        order.getItems().forEach(item ->
                inventoryRepository.incrementStock(item.getProductId(), item.getQuantity())
        );

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Order cancelled by customer.");
                payment.setAdminNotes("Order cancelled by customer.");
                paymentRepository.save(payment);
            }
        });

        if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        orderRepository.save(order);

        log.info("Order cancelled: {} by user: {}", order.getOrderNumber(), userId);

        return ApiResponse.success(
                "Order #" + order.getOrderNumber() + " has been cancelled.",
                toOrderResponse(order)
        );
    }

    // ── Admin Operations ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return PagedResponse.from(
                orderRepository.findAllByOrderByCreatedAtDesc(pageable),
                this::toOrderResponse
        );
    }

    @Override
    public ApiResponse<OrderResponse> updateOrderStatus(Long orderId, OrderStatus status, String adminNote) {
        if (status == null) {
            throw new BusinessException("Order status is required.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus previousStatus = order.getStatus();

        if (previousStatus == OrderStatus.CANCELLED) {
            throw new BusinessException("Cancelled orders cannot be updated.");
        }

        if (previousStatus == OrderStatus.DELIVERED && status != OrderStatus.DELIVERED) {
            throw new BusinessException("Delivered orders cannot be moved back to another status.");
        }

        if (status == OrderStatus.PENDING && previousStatus != OrderStatus.PENDING) {
            throw new BusinessException("Order cannot be moved back to PENDING.");
        }

        if (status != OrderStatus.CANCELLED && order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException("Payment is not verified yet. Verify payment before changing order status.");
        }

        order.setStatus(status);

        if (adminNote != null && !adminNote.isBlank()) {
            order.setAdminNote(adminNote);
        }

        switch (status) {
            case CONFIRMED -> {
                if (order.getConfirmedAt() == null) {
                    order.setConfirmedAt(LocalDateTime.now());
                }
            }

            case SHIPPED -> {
                if (order.getConfirmedAt() == null) {
                    order.setConfirmedAt(LocalDateTime.now());
                }
                order.setShippedAt(LocalDateTime.now());
            }

            case DELIVERED -> {
                if (order.getConfirmedAt() == null) {
                    order.setConfirmedAt(LocalDateTime.now());
                }

                if (order.getShippedAt() == null) {
                    order.setShippedAt(LocalDateTime.now());
                }

                order.setDeliveredAt(LocalDateTime.now());
            }

            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());

                if (previousStatus != OrderStatus.CANCELLED) {
                    order.getItems().forEach(item ->
                            inventoryRepository.incrementStock(item.getProductId(), item.getQuantity())
                    );
                }

                if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
                    order.setPaymentStatus(PaymentStatus.FAILED);

                    paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                        if (payment.getStatus() != PaymentStatus.SUCCESS) {
                            payment.setStatus(PaymentStatus.FAILED);
                            payment.setFailureReason(
                                    adminNote != null && !adminNote.isBlank()
                                            ? adminNote
                                            : "Order cancelled by admin."
                            );
                            payment.setAdminNotes("Order cancelled by admin.");
                            paymentRepository.save(payment);
                        }
                    });
                }
            }

            default -> {
            }
        }

        orderRepository.save(order);

        return ApiResponse.success(
                "Order status updated to " + status.name(),
                toOrderResponse(order)
        );
    }

    @Override
    public ApiResponse<OrderResponse> updatePaymentStatus(Long orderId, String transactionId, String adminEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot verify payment for a cancelled order.");
        }

        if (isRazorpayPayment(order)) {
            throw new BusinessException("Card/Razorpay payments cannot be manually verified from Orders page. Real gateway confirmation will be added during production setup.");
        }

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("Payment is already verified for this order.");
        }

        if (transactionId == null || transactionId.trim().isBlank()) {
            throw new BusinessException("Transaction ID / UTR number is required.");
        }

        String cleanTransactionId = transactionId.trim();

        if (isUpiPayment(order) && !cleanTransactionId.matches("^\\d{12}$")) {
            throw new BusinessException("Please enter a valid 12-digit UTR number for UPI payment.");
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setPaymentTransactionId(cleanTransactionId);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(LocalDateTime.now());
        }

        orderRepository.save(order);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);

            if (payment.getUtrNumber() == null || payment.getUtrNumber().isBlank()) {
                payment.setUtrNumber(cleanTransactionId);
            }

            payment.setFailureReason(null);
            payment.setVerifiedAt(LocalDateTime.now());
            payment.setVerifiedBy(adminEmail);
            payment.setAdminNotes("Verified via order payment API.");
            paymentRepository.save(payment);
        });

        return ApiResponse.success(
                "Payment verified for order #" + order.getOrderNumber(),
                toOrderResponse(order)
        );
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

    private String generateOrderNumber() {
        long count = orderRepository.countAllOrders() + 1;
        return String.format("RLJ-%d-%06d", Year.now().getValue(), count);
    }

    private void validateServiceablePincode(String state) {
        List<String> serviceableStates = List.of("Bihar", "Jharkhand");

        if (!serviceableStates.stream().anyMatch(s -> s.equalsIgnoreCase(state))) {
            throw new BusinessException(
                    "Sorry, we currently deliver only to Bihar and Jharkhand. " +
                            "Your address state '" + state + "' is not serviceable yet."
            );
        }
    }

    private boolean isUpiPayment(Order order) {
        return order.getPaymentMethod() != null
                && order.getPaymentMethod().name().startsWith("UPI");
    }

    private boolean isRazorpayPayment(Order order) {
        return order.getPaymentMethod() != null
                && order.getPaymentMethod().name().equalsIgnoreCase("RAZORPAY");
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