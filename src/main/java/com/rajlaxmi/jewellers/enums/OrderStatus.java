package com.rajlaxmi.jewellers.enums;

/**
 * Order lifecycle states.
 *
 * Flow: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *   or: PENDING → CANCELLED
 *   or: DELIVERED → RETURN_REQUESTED → RETURNED
 *
 * PENDING     = Order placed, awaiting payment confirmation
 * CONFIRMED   = Payment received, order accepted
 * PROCESSING  = Jewellery being packed/prepared
 * SHIPPED     = Dispatched to delivery partner
 * DELIVERED   = Customer received the order
 * CANCELLED   = Cancelled before shipment
 * RETURN_REQUESTED = Customer initiated return
 * RETURNED    = Return processed, refund initiated
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED
}
