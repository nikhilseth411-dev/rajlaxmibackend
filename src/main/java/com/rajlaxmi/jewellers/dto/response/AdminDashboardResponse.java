package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/** Admin dashboard statistics response */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminDashboardResponse {
    private long totalProducts;
    private long activeProducts;
    private long outOfStockProducts;
    private long lowStockProducts;
    private long totalUsers;
    private long newUsersToday;
    private long totalCategories;
    private BigDecimal currentGold22KRate;
    private List<ProductResponse> lowStockAlerts;
    private List<ProductResponse> recentProducts;
}
