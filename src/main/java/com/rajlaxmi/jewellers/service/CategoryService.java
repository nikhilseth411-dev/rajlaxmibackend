package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.CreateCategoryRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getRootCategories();
    CategoryResponse getCategoryBySlug(String slug);
    CategoryResponse getCategoryById(Long id);
    ApiResponse<CategoryResponse> createCategory(CreateCategoryRequest request);
    ApiResponse<CategoryResponse> updateCategory(Long id, CreateCategoryRequest request);
    ApiResponse<String> deleteCategory(Long id);
}
