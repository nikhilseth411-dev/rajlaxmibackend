package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StoreVisitResponse {
    private Long id;
    private LocalDate visitDate;
    private String timeSlot;
    private String status;
    private String purposeNote;
    private String contactPhone;
    private String adminNote;
    private LocalDateTime createdAt;
}
