package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.CreateCategoryRequest;
import com.rajlaxmi.jewellers.dto.request.CreateProductRequest;
import com.rajlaxmi.jewellers.dto.request.GoldPriceOverrideRequest;
import com.rajlaxmi.jewellers.dto.request.UpdateInventoryRequest;
import com.rajlaxmi.jewellers.dto.request.UpdateAdminCredentialsRequest;
import com.rajlaxmi.jewellers.dto.request.UpdateProductRequest;
import com.rajlaxmi.jewellers.dto.response.*;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.repository.ProductRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AdminController — /admin/**
 * ALL endpoints require ADMIN role.
 * @PreAuthorize("hasRole('ADMIN')") is enforced at method level.
 * Additionally protected at URL level in SecurityConfig.
 * Double protection = defense in depth.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only product, category, and dashboard management")
@SecurityRequirement(name = "BearerAuth")
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final GoldPriceService goldPriceService;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AdminAccountService adminAccountService;

    // ── Dashboard ─────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard statistics")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        GoldRateResponse goldRate = goldPriceService.getCurrentRates();
        List<ProductResponse> lowStock = productRepository.findLowStockProducts()
                .stream().limit(10).map(p -> ProductResponse.builder()
                        .id(p.getId()).name(p.getName()).sku(p.getSku())
                        .stockQuantity(p.getInventory() != null ? p.getInventory().getQuantity() : 0)
                        .build()).toList();

        AdminDashboardResponse dashboard = AdminDashboardResponse.builder()
                .totalProducts(productRepository.count())
                .activeProducts(productRepository.countByIsActiveTrue())
                .outOfStockProducts(productRepository.findOutOfStockProducts().size())
                .lowStockProducts(lowStock.size())
                .totalUsers(userRepository.count())
                .newUsersToday(userRepository.countByCreatedAtAfter(
                        java.time.LocalDateTime.now().withHour(0).withMinute(0)))
                .currentGold22KRate(goldRate.getRate22K())
                .lowStockAlerts(lowStock)
                .build();

        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PutMapping("/account/credentials")
    @Operation(summary = "Update the authenticated admin email and password")
    public ResponseEntity<ApiResponse<UserResponse>> updateCredentials(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody UpdateAdminCredentialsRequest request) {
        return ResponseEntity.ok(adminAccountService.updateCredentials(admin, request));
    }

    // ── Products ──────────────────────────────────────────────

    @PostMapping("/products")
    @Operation(summary = "Create new product")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PatchMapping("/products/{id}")
    @Operation(summary = "Update product fields")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Deactivate product (soft delete)")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @PostMapping(value = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product image")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        return ResponseEntity.ok(productService.uploadProductImage(id, file, isPrimary));
    }

    @DeleteMapping("/products/{productId}/images/{imageId}")
    @Operation(summary = "Delete product image")
    public ResponseEntity<ApiResponse<String>> deleteImage(
            @PathVariable Long productId, @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.deleteProductImage(productId, imageId));
    }

    @PostMapping(value = "/products/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk import products via CSV file")
    public ResponseEntity<ApiResponse<String>> bulkImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productService.bulkImportFromCsv(file));
    }

    // ── Categories ────────────────────────────────────────────

    @PostMapping("/categories")
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Deactivate category")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.deleteCategory(id));
    }

    // ── Gold Price Override ────────────────────────────────────

    @PostMapping("/gold-rates/override")
    @Operation(summary = "Manually override current gold rate")
    public ResponseEntity<ApiResponse<GoldRateResponse>> overrideGoldRate(
            @Valid @RequestBody GoldPriceOverrideRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(goldPriceService.adminOverridePrice(request));
    }

    @DeleteMapping("/gold-rates/override")
    @Operation(summary = "Remove gold rate override (revert to API)")
    public ResponseEntity<ApiResponse<String>> removeOverride() {
        return ResponseEntity.ok(goldPriceService.removeAdminOverride());
    }

    // ── Inventory ─────────────────────────────────────────────

    @GetMapping("/inventory/{productId}")
    @Operation(summary = "Get inventory for a product")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(productId)));
    }

    @PutMapping("/inventory")
    @Operation(summary = "Create or update product inventory")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @Valid @RequestBody UpdateInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventory(request));
    }

    @GetMapping("/inventory/low-stock")
    @Operation(summary = "Get all low-stock products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStock() {
        var gp = goldPriceService.getCurrentGoldPriceEntity();
        var products = productRepository.findLowStockProducts().stream()
                .map(p -> ProductResponse.builder().id(p.getId()).name(p.getName()).sku(p.getSku())
                        .stockQuantity(p.getInventory() != null ? p.getInventory().getQuantity() : 0)
                        .isInStock(true).build()).toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/inventory/out-of-stock")
    @Operation(summary = "Get all out-of-stock products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getOutOfStock() {
        var products = productRepository.findOutOfStockProducts().stream()
                .map(p -> ProductResponse.builder().id(p.getId()).name(p.getName())
                        .sku(p.getSku()).isInStock(false).stockQuantity(0).build()).toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
