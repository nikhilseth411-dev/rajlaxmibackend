package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private int sortOrder;
    private boolean isActive;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> children;
    private long productCount;
}
