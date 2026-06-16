package com.rajlaxmi.jewellers.dto.request;

import com.rajlaxmi.jewellers.enums.GoldPurity;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import lombok.Data;

/**
 * ProductFilterRequest — query params for GET /products
 * All fields optional — only non-null fields are applied as filters.
 * Used for the product listing / search / filter page.
 */
@Data
public class ProductFilterRequest {
    private String keyword;          // search in name/description
    private Long categoryId;
    private ProductCategory productCategory;
    private GoldPurity goldPurity;
    private String metalType;
    private String occasion;
    private String gender;
    private Boolean isFeatured;
    private Boolean isNewArrival;
    private Boolean isBestSeller;
    private int page = 0;
    private int size = 12;
    private String sortBy = "createdAt";    // createdAt, name, weight
    private String sortDir = "desc";        // asc, desc
}
