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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${business.upi-id:329171694663381@cnrb}")
    private String businessUpiId;

    @Value("${business.name:RajLaxmi Jewellers}")
    private String businessName;

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

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<String> getUpiQrCode(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this payment QR code.");
        }

        if (!isUpiPayment(order)) {
            throw new BusinessException("QR code is available only for UPI QR payments.");
        }

        String qrUrl = generateQrCodeUrl(order);
        return ApiResponse.success("QR code URL generated.", qrUrl);
    }

    public String generateQrCodeUrl(Order order) {
        if (!isUpiPayment(order)) {
            return null;
        }

        String upiLink = buildUpiDeepLink(order);
        String encoded = URLEncoder.encode(upiLink, StandardCharsets.UTF_8);
        return "https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=" + encoded;
    }

    public String buildUpiDeepLink(Order order) {
        String encodedBusinessName = URLEncoder.encode(businessName, StandardCharsets.UTF_8);
        String encodedNote = URLEncoder.encode("Order " + order.getOrderNumber(), StandardCharsets.UTF_8);

        return String.format(
                "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s",
                businessUpiId,
                encodedBusinessName,
                order.getGrandTotal(),
                encodedNote
        );
    }

    @Override
    public ApiResponse<PaymentResponse> submitUtrNumber(Long orderId, Long userId, String utrNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this payment.");
        }

        if (!isUpiPayment(order)) {
            throw new BusinessException("UTR submission is allowed only for UPI QR payments.");
        }

        if (utrNumber == null || !utrNumber.trim().matches("^\\d{12}$")) {
            throw new BusinessException("Please enter a valid 12-digit UTR number.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("Payment is already confirmed.");
        }

        if (payment.getStatus() == PaymentStatus.PENDING_VERIFICATION) {
            throw new BusinessException("UTR is already submitted and waiting for admin verification.");
        }

        String cleanUtr = utrNumber.trim();

        if (paymentRepository.existsByUtrNumber(cleanUtr)) {
            throw new BusinessException("This UTR number has already been submitted. If this is an error, contact support.");
        }

        payment.setUtrNumber(cleanUtr);
        payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        payment.setFailureReason(null);
        payment.setAdminNotes(null);
        payment.setVerifiedAt(null);
        payment.setVerifiedBy(null);
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PENDING_VERIFICATION);
        orderRepository.save(order);

        log.info("UTR submitted for order {}: {}", order.getOrderNumber(), cleanUtr);

        return ApiResponse.success(
                "UTR submitted successfully. Admin will verify your payment within 2-4 hours.",
                toPaymentResponse(payment, order)
        );
    }

    @Override
    public ApiResponse<PaymentResponse> adminConfirmPayment(Long paymentId, PaymentConfirmRequest request, String adminEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        Order order = payment.getOrder();

        if ("CONFIRM".equalsIgnoreCase(request.getAction())) {
            if (payment.getStatus() != PaymentStatus.PENDING_VERIFICATION) {
                throw new BusinessException("Only payments pending verification can be confirmed.");
            }

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVerifiedAt(LocalDateTime.now());
            payment.setVerifiedBy(adminEmail);
            payment.setAdminNotes(request.getAdminNotes());
            payment.setFailureReason(null);

            order.setPaymentStatus(PaymentStatus.SUCCESS);

            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setConfirmedAt(LocalDateTime.now());
            }

            orderRepository.save(order);

            log.info("Payment CONFIRMED by admin {} for order {}", adminEmail, order.getOrderNumber());

        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            if (payment.getStatus() != PaymentStatus.PENDING_VERIFICATION) {
                throw new BusinessException("Only payments pending verification can be rejected.");
            }

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

        return ApiResponse.success(
                "Payment " + request.getAction().toLowerCase() + "ed.",
                toPaymentResponse(payment, order)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PaymentResponse>> getPendingPayments() {
        List<PaymentResponse> pending = paymentRepository
                .findByStatus(PaymentStatus.PENDING_VERIFICATION)
                .stream()
                .map(payment -> toPaymentResponse(payment, payment.getOrder()))
                .toList();

        return ApiResponse.success(pending);
    }

    @Override
    public Payment createPaymentForOrder(Order order) {
        boolean upiPayment = isUpiPayment(order);

        String qrUrl = null;
        String upiId = null;

        if (upiPayment) {
            upiId = businessUpiId;
            qrUrl = generateQrCodeUrl(order);
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(order.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .amount(order.getGrandTotal())
                .upiId(upiId)
                .upiQrImageUrl(qrUrl)
                .build();

        return paymentRepository.save(payment);
    }

    private PaymentResponse toPaymentResponse(Payment payment, Order order) {
        String upiLink = null;

        if (isUpiPayment(order)) {
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

    private boolean isUpiPayment(Order order) {
        return order.getPaymentMethod() != null
                && order.getPaymentMethod().name().startsWith("UPI");
    }
}