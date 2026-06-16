package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /admin/categories */
@Data
public class CreateCategoryRequest {
    @NotBlank @Size(min = 2, max = 100)
    private String name;

    @Size(max = 300)
    private String description;

    private String imageUrl;
    private Long parentId;       // null = top-level category
    private int sortOrder = 0;
    private boolean isActive = true;
}
