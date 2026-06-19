package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.PaymentConfirmRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PaymentResponse;
import com.rajlaxmi.jewellers.entity.Order;
import com.rajlaxmi.jewellers.entity.Payment;

public interface PaymentService {

    /** Get payment details for an order */
    PaymentResponse getPaymentByOrderId(Long orderId, Long userId);

    /** Customer submits UTR after UPI payment */
    ApiResponse<PaymentResponse> submitUtrNumber(Long orderId, Long userId, String utrNumber);

    /** Admin confirms/rejects a payment */
    ApiResponse<PaymentResponse> adminConfirmPayment(Long paymentId, PaymentConfirmRequest request, String adminEmail);

    /** Get UPI QR code URL for checkout */
    ApiResponse<String> getUpiQrCode(Long orderId);

    /** Get all pending UPI payments (admin) */
    ApiResponse<?> getPendingPayments();

    /** Create the payment record that belongs to a newly placed order */
    Payment createPaymentForOrder(Order order);
}
