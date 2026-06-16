package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.CreateProductRequest;
import com.rajlaxmi.jewellers.dto.request.ProductFilterRequest;
import com.rajlaxmi.jewellers.dto.request.UpdateProductRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PagedResponse;
import com.rajlaxmi.jewellers.dto.response.ProductDetailResponse;
import com.rajlaxmi.jewellers.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    PagedResponse<ProductResponse> getProducts(ProductFilterRequest filter);
    PagedResponse<ProductResponse> searchProducts(String keyword, int page, int size);
    ProductDetailResponse getProductById(Long id);
    ProductDetailResponse getProductBySlug(String slug);
    List<ProductResponse> getFeaturedProducts();
    List<ProductResponse> getNewArrivals();
    List<ProductResponse> getBestSellers();
    ApiResponse<ProductDetailResponse> createProduct(CreateProductRequest request);
    ApiResponse<ProductDetailResponse> updateProduct(Long id, UpdateProductRequest request);
    ApiResponse<String> deleteProduct(Long id);
    ApiResponse<String> uploadProductImage(Long productId, MultipartFile file, boolean isPrimary);
    ApiResponse<String> deleteProductImage(Long productId, Long imageId);
    ApiResponse<String> bulkImportFromCsv(MultipartFile csvFile);
}
