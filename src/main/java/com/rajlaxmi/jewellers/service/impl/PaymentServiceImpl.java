package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.PaymentConfirmRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PaymentResponse;
import com.rajlaxmi.jewellers.entity.Order;
import com.rajlaxmi.jewellers.entity.Payment;
import com.rajlaxmi.jewellers.enums.OrderStatus;
import com.rajlaxmi.jewellers.enums.PaymentStatus;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.exception.UnauthorizedException;
import com.rajlaxmi.jewellers.repository.OrderRepository;
import com.rajlaxmi.jewellers.repository.PaymentRepository;
import com.rajlaxmi.jewellers.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ================================================================
 * PaymentServiceImpl — UPI + COD Payment Handling
 * ================================================================
 *
 * BUSINESS CONTEXT:
 *   RajLaxmi Jewellers (Wazirganj, Gaya, Bihar) primarily uses UPI
 *   for digital payments. The UPI ID is: nitinseth753@okhdfcbank
 *
 * UPI PAYMENT FLOW:
 *   1. Customer places order (PaymentMethod = UPI_QR)
 *   2. Backend generates UPI deep link from order amount + UPI ID
 *   3. QR code URL is returned to frontend for display
 *   4. Customer scans QR, pays in any UPI app (GPay, PhonePe, BHIM)
 *   5. Customer enters UTR (12-digit transaction reference) in app
 *   6. Admin sees pending payment in dashboard
 *   7. Admin confirms receipt → order status moves to CONFIRMED
 *
 * UPI DEEP LINK FORMAT:
 *   upi://pay?pa=<upiId>&pn=<name>&am=<amount>&cu=INR&tn=<note>
 *   This format is supported by all Indian UPI apps.
 *
 * QR CODE:
 *   We use Google Charts API to generate QR from the deep link.
 *   URL: https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=<encoded_upi_link>
 *   This is free, no API key, works reliably.
 *
 * COD FLOW:
 *   Cash on delivery is also supported for Bihar/Jharkhand.
 *   No payment verification needed — marked PAID on delivery.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${business.upi-id:nitinseth753@okhdfcbank}")
    private String businessUpiId;

    @Value("${business.name:RajLaxmi Jewellers}")
    private String businessName;

    // ── Get Payment for Order ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this payment.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        return toPaymentResponse(payment, order);
    }

    // ── UPI QR Code Generation ────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<String> getUpiQrCode(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        String qrUrl = generateQrCodeUrl(order);
        return ApiResponse.success("QR code URL generated.", qrUrl);
    }

    /**
     * Generates a Google Charts QR code URL for the UPI payment.
     *
     * The QR encodes a UPI deep link:
     *   upi://pay?pa=nitinseth753@okhdfcbank&pn=RajLaxmi+Jewellers
     *              &am=59053.00&cu=INR&tn=Order+RLJ-2025-000001
     *
     * This QR can be scanned by any Indian UPI app to pre-fill payment.
     *
     * NOTE: The UPI QR from the uploaded image (nitinseth753@okhdfcbank)
     * is used as the static QR for display. Dynamic QR is generated per order.
     */
    public String generateQrCodeUrl(Order order) {
        String upiLink = buildUpiDeepLink(order);
        String encoded = URLEncoder.encode(upiLink, StandardCharsets.UTF_8);
        return "https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=" + encoded;
    }

    /**
     * UPI Deep Link format (works with GPay, PhonePe, BHIM, Paytm):
     * upi://pay?pa=<upi_id>&pn=<payee_name>&am=<amount>&cu=INR&tn=<note>
     *
     * pa = Payment Address (UPI ID)
     * pn = Payee Name (business name)
     * am = Amount (2 decimal places)
     * cu = Currency (INR)
     * tn = Transaction Note (order number)
     */
    public String buildUpiDeepLink(Order order) {
        return String.format(
            "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=Order%%20%s",
            businessUpiId,
            businessName.replace(" ", "+"),
            order.getGrandTotal(),
            order.getOrderNumber()
        );
    }

    // ── Submit UTR After Payment ──────────────────────────────

    @Override
    public ApiResponse<PaymentResponse> submitUtrNumber(Long orderId, Long userId, String utrNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this payment.");
        }

        // Check UTR not already used
        if (paymentRepository.existsByUtrNumber(utrNumber.trim())) {
            throw new BusinessException("This UTR number has already been submitted. If this is an error, contact support.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("Payment is already confirmed.");
        }

        payment.setUtrNumber(utrNumber.trim());
        payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        paymentRepository.save(payment);

        log.info("UTR submitted for order {}: {}", order.getOrderNumber(), utrNumber);
        return ApiResponse.success("UTR submitted. Admin will verify within 2-4 hours.", toPaymentResponse(payment, order));
    }

    // ── Admin Confirm/Reject Payment ──────────────────────────

    @Override
    public ApiResponse<PaymentResponse> adminConfirmPayment(Long paymentId, PaymentConfirmRequest request, String adminEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        Order order = payment.getOrder();

        if ("CONFIRM".equalsIgnoreCase(request.getAction())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVerifiedAt(LocalDateTime.now());
            payment.setVerifiedBy(adminEmail);
            payment.setAdminNotes(request.getAdminNotes());

            // Update order status
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(LocalDateTime.now());
            orderRepository.save(order);

            log.info("Payment CONFIRMED by admin {} for order {}", adminEmail, order.getOrderNumber());

        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(request.getAdminNotes());
            payment.setAdminNotes(request.getAdminNotes());

            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            log.info("Payment REJECTED by admin {} for order {}", adminEmail, order.getOrderNumber());

        } else {
            throw new BusinessException("Invalid action. Use CONFIRM or REJECT.");
        }

        paymentRepository.save(payment);
        return ApiResponse.success("Payment " + request.getAction().toLowerCase() + "ed.", toPaymentResponse(payment, order));
    }

    // ── Admin: Pending Payments ───────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PaymentResponse>> getPendingPayments() {
        List<PaymentResponse> pending = paymentRepository
                .findByStatus(PaymentStatus.PENDING_VERIFICATION)
                .stream()
                .map(p -> toPaymentResponse(p, p.getOrder()))
                .toList();
        return ApiResponse.success(pending);
    }

    // ── Helpers ───────────────────────────────────────────────

    public Payment createPaymentForOrder(Order order) {
        String upiLink = buildUpiDeepLink(order);
        String qrUrl = null;
        try {
            String encoded = URLEncoder.encode(upiLink, StandardCharsets.UTF_8);
            qrUrl = "https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=" + encoded;
        } catch (Exception e) {
            log.warn("Failed to generate QR URL for order {}", order.getOrderNumber());
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(order.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .amount(order.getGrandTotal())
                .upiId(businessUpiId)
                .upiQrImageUrl(qrUrl)
                .build();

        return paymentRepository.save(payment);
    }

    private PaymentResponse toPaymentResponse(Payment payment, Order order) {
        String upiLink = null;
        if (order.getPaymentMethod() != null &&
                order.getPaymentMethod().name().startsWith("UPI")) {
            upiLink = buildUpiDeepLink(order);
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .upiId(payment.getUpiId())
                .upiQrImageUrl(payment.getUpiQrImageUrl())
                .utrNumber(payment.getUtrNumber())
                .verifiedAt(payment.getVerifiedAt())
                .verifiedBy(payment.getVerifiedBy())
                .adminNotes(payment.getAdminNotes())
                .failureReason(payment.getFailureReason())
                .upiDeepLink(upiLink)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
