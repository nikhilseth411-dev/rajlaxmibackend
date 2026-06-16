package com.rajlaxmi.jewellers.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private String altText;
    private boolean isPrimary;
    private int sortOrder;
}
