package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentConfirmRequest {

    @NotNull(message = "Action is required: CONFIRM or REJECT")
    private String action; // "CONFIRM" or "REJECT"

    private String adminNotes;
}
