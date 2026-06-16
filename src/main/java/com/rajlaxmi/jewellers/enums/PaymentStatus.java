package com.rajlaxmi.jewellers.enums;

/**
 * Payment transaction status.
 * PENDING_VERIFICATION: UPI payment done by customer, awaiting admin verification.
 */
public enum PaymentStatus {
    PENDING,             // payment initiated, awaiting action
    PENDING_VERIFICATION, // UTR submitted by customer, admin must verify
    SUCCESS,             // payment received and verified
    FAILED,              // payment attempt failed or rejected
    REFUND_PENDING,      // refund initiated
    REFUNDED             // refund completed
}
