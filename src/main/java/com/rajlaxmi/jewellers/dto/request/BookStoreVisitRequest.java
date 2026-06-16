package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookStoreVisitRequest {
    @NotNull(message = "Visit date is required")
    @FutureOrPresent(message = "Visit date must be today or in the future")
    private LocalDate visitDate;

    @NotBlank(message = "Time slot is required")
    private String timeSlot; // "10:00-11:00"

    @Pattern(regexp = "^[6-9]\\d{9}$")
    private String contactPhone;

    private String purposeNote;
}
