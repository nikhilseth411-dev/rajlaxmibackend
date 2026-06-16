package com.rajlaxmi.jewellers.dto.request;

import com.rajlaxmi.jewellers.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotNull(message = "Shipping address ID is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 20)
    private String couponCode;

    @Size(max = 500)
    private String customerNote;
}
