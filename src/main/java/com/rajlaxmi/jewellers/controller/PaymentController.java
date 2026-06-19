package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.PaymentConfirmRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PaymentResponse;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * PaymentController — /payments/**
 *
 * UPI Payment flow endpoints:
 *   GET  /payments/orders/{orderId}           → get payment details + QR code
 *   POST /payments/orders/{orderId}/submit-utr → customer submits UTR
 *   GET  /payments/admin/pending              → admin: list pending payments
 *   POST /payments/admin/{paymentId}/confirm  → admin: confirm or reject
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "UPI payment management and verification")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    // ── Customer Endpoints ────────────────────────────────────

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get payment details for an order (includes UPI QR code)")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/orders/{orderId}/submit-utr")
    @Operation(summary = "Submit UTR number after UPI payment",
               description = "Customer submits the 12-digit UTR from their UPI app. Admin will verify within 2-4 hours.")
    public ResponseEntity<ApiResponse<PaymentResponse>> submitUtr(
            @PathVariable Long orderId,
            @RequestParam String utrNumber,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.submitUtrNumber(orderId, user.getId(), utrNumber));
    }

    @GetMapping("/orders/{orderId}/qr-code")
    @Operation(summary = "Get UPI QR code URL for payment",
               description = "Returns Google Charts QR URL encoding the UPI deep link for nitinseth753@okhdfcbank")
    public ResponseEntity<ApiResponse<String>> getQrCode(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.getUpiQrCode(orderId, user.getId()));
    }

    // ── Admin Endpoints ────────────────────────────────────────

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments pending verification (admin)")
    public ResponseEntity<ApiResponse<?>> getPendingPayments() {
        return ResponseEntity.ok(paymentService.getPendingPayments());
    }

    @PostMapping("/admin/{paymentId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin confirms or rejects a payment",
               description = "action = CONFIRM or REJECT. On CONFIRM, order status moves to CONFIRMED.")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentConfirmRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(paymentService.adminConfirmPayment(paymentId, request, admin.getEmail()));
    }
}
