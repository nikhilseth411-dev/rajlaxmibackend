package com.rajlaxmi.jewellers.enums;

/**
 * Supported payment methods.
 * UPI_QR → QR code scan with nitinseth753@okhdfcbank
 * RAZORPAY → cards, net banking, UPI via Razorpay gateway (V2)
 * COD → Cash on Delivery
 * BANK_TRANSFER → direct bank transfer
 */
public enum PaymentMethod {
    UPI_QR,
    RAZORPAY,
    COD,
    BANK_TRANSFER
}
