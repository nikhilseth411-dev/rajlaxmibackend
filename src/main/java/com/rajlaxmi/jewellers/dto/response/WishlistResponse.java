package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WishlistResponse {
    private List<ProductResponse> products;
    private int totalItems;
}
