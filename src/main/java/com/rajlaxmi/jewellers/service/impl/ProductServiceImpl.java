package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.CreateProductRequest;
import com.rajlaxmi.jewellers.dto.request.ProductFilterRequest;
import com.rajlaxmi.jewellers.dto.request.UpdateProductRequest;
import com.rajlaxmi.jewellers.dto.response.*;
import com.rajlaxmi.jewellers.entity.*;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.DuplicateResourceException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.*;
import com.rajlaxmi.jewellers.service.GoldPriceService;
import com.rajlaxmi.jewellers.service.ProductService;
import com.rajlaxmi.jewellers.util.PricingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final GoldPriceService goldPriceService;
    private final PricingEngine pricingEngine;

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "name", "sku", "weightGrams", "makingCharges"
    );

    // ── Browse / Search ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(ProductFilterRequest filter) {
        ProductFilterRequest effectiveFilter = filter != null ? filter : new ProductFilterRequest();
        Pageable pageable = buildPageable(
                effectiveFilter.getPage(),
                effectiveFilter.getSize(),
                effectiveFilter.getSortBy(),
                effectiveFilter.getSortDir()
        );

        String keyword = normalizeLowerFilter(effectiveFilter.getKeyword());
        if (keyword == null) {
            keyword = "";
        }

        Page<Product> page = productRepository.findActiveByFilters(
                keyword,
                effectiveFilter.getCategoryId(),
                effectiveFilter.getProductCategory(),
                effectiveFilter.getGoldPurity(),
                normalizeLowerFilter(effectiveFilter.getMetalType()),
                normalizeLowerFilter(effectiveFilter.getOccasion()),
                normalizeLowerFilter(effectiveFilter.getGender()),
                effectiveFilter.getIsFeatured(),
                effectiveFilter.getIsNewArrival(),
                effectiveFilter.getIsBestSeller(),
                pageable
        );
        GoldPrice goldPrice = goldPriceService.getCurrentGoldPriceEntity();
        return PagedResponse.from(page, p -> toProductResponse(p, goldPrice));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        String normalizedKeyword = normalizeLowerFilter(keyword);
        if (normalizedKeyword == null) {
            throw new BusinessException("Search keyword is required.");
        }

        Pageable pageable = buildPageable(page, size, "createdAt", "desc");
        Page<Product> result = productRepository.searchProducts(normalizedKeyword, pageable);
        GoldPrice goldPrice = goldPriceService.getCurrentGoldPriceEntity();
        return PagedResponse.from(result, p -> toProductResponse(p, goldPrice));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return toDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugWithImages(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return toDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts() {
        GoldPrice gp = goldPriceService.getCurrentGoldPriceEntity();
        return productRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(p -> toProductResponse(p, gp)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals() {
        GoldPrice gp = goldPriceService.getCurrentGoldPriceEntity();
        return productRepository.findByIsNewArrivalTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(p -> toProductResponse(p, gp)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getBestSellers() {
        GoldPrice gp = goldPriceService.getCurrentGoldPriceEntity();
        return productRepository.findByIsBestSellerTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(p -> toProductResponse(p, gp)).toList();
    }

    // ── Admin CRUD ────────────────────────────────────────────

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public ApiResponse<ProductDetailResponse> createProduct(CreateProductRequest req) {
        if (productRepository.existsBySku(req.getSku())) {
            throw new DuplicateResourceException("Product", "SKU", req.getSku());
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", req.getCategoryId()));

        String slug = generateProductSlug(req.getName(), req.getSku());

        Product product = Product.builder()
                .name(req.getName().trim())
                .sku(req.getSku().toUpperCase())
                .description(req.getDescription())
                .category(category)
                .productCategory(req.getProductCategory())
                .metalType(req.getMetalType())
                .goldPurity(req.getGoldPurity())
                .weightGrams(req.getWeightGrams())
                .makingCharges(req.getMakingCharges())
                .makingChargesType(req.getMakingChargesType() != null ? req.getMakingChargesType() : "PER_GRAM")
                .stoneCharges(req.getStoneCharges() != null ? req.getStoneCharges() : BigDecimal.ZERO)
                .gstPercentage(req.getGstPercentage() != null ? req.getGstPercentage() : new BigDecimal("3.00"))
                .isBisHallmarked(req.isBisHallmarked())
                .bisHallmarkNumber(req.getBisHallmarkNumber())
                .occasion(req.getOccasion())
                .gender(req.getGender())
                .dimensions(req.getDimensions())
                .finish(req.getFinish())
                .isFeatured(req.isFeatured())
                .isNewArrival(req.isNewArrival())
                .isBestSeller(req.isBestSeller())
                .isActive(true)
                .metaTitle(req.getMetaTitle() != null ? req.getMetaTitle() : req.getName())
                .metaDescription(req.getMetaDescription())
                .slug(slug)
                .build();

        productRepository.save(product);

        // Create inventory record
        Inventory inventory = Inventory.builder()
                .product(product)
                .quantity(req.getStockQuantity())
                .lowStockThreshold(req.getLowStockThreshold())
                .isInStock(req.getStockQuantity() > 0)
                .build();
        inventoryRepository.save(inventory);
        product.setInventory(inventory);

        log.info("Product created: {} ({})", product.getName(), product.getSku());
        return ApiResponse.success("Product created successfully.", toDetailResponse(product));
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public ApiResponse<ProductDetailResponse> updateProduct(Long id, UpdateProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (req.getName() != null) product.setName(req.getName().trim());
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getCategoryId() != null) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", req.getCategoryId()));
            product.setCategory(cat);
        }
        if (req.getProductCategory() != null) product.setProductCategory(req.getProductCategory());
        if (req.getMetalType() != null) product.setMetalType(req.getMetalType());
        if (req.getGoldPurity() != null) product.setGoldPurity(req.getGoldPurity());
        if (req.getWeightGrams() != null) product.setWeightGrams(req.getWeightGrams());
        if (req.getMakingCharges() != null) product.setMakingCharges(req.getMakingCharges());
        if (req.getMakingChargesType() != null) product.setMakingChargesType(req.getMakingChargesType());
        if (req.getStoneCharges() != null) product.setStoneCharges(req.getStoneCharges());
        if (req.getGstPercentage() != null) product.setGstPercentage(req.getGstPercentage());
        if (req.getIsBisHallmarked() != null) product.setBisHallmarked(req.getIsBisHallmarked());
        if (req.getBisHallmarkNumber() != null) product.setBisHallmarkNumber(req.getBisHallmarkNumber());
        if (req.getOccasion() != null) product.setOccasion(req.getOccasion());
        if (req.getGender() != null) product.setGender(req.getGender());
        if (req.getDimensions() != null) product.setDimensions(req.getDimensions());
        if (req.getFinish() != null) product.setFinish(req.getFinish());
        if (req.getIsFeatured() != null) product.setFeatured(req.getIsFeatured());
        if (req.getIsNewArrival() != null) product.setNewArrival(req.getIsNewArrival());
        if (req.getIsBestSeller() != null) product.setBestSeller(req.getIsBestSeller());
        if (req.getIsActive() != null) product.setActive(req.getIsActive());
        if (req.getMetaTitle() != null) product.setMetaTitle(req.getMetaTitle());
        if (req.getMetaDescription() != null) product.setMetaDescription(req.getMetaDescription());

        productRepository.save(product);
        return ApiResponse.success("Product updated successfully.", toDetailResponse(product));
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public ApiResponse<String> deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setActive(false);
        productRepository.save(product);
        return ApiResponse.success("Product deactivated successfully.");
    }

    @Override
    public ApiResponse<String> uploadProductImage(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Only image files (JPEG, PNG, WebP) are allowed.");
        }

        try {
            // Save file to uploads directory
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir + "products/");
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "uploads/products/" + filename;
            int sortOrder = product.getImages().size();

            // If primary, unset other primary images
            if (isPrimary) {
                product.getImages().forEach(img -> img.setPrimary(false));
            }

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .altText(product.getName() + " - image " + (sortOrder + 1))
                    .isPrimary(isPrimary || product.getImages().isEmpty())
                    .sortOrder(sortOrder)
                    .build();

            product.getImages().add(image);
            productRepository.save(product);
            return ApiResponse.success("Image uploaded successfully.", imageUrl);

        } catch (Exception e) {
            throw new BusinessException("Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<String> deleteProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        product.getImages().removeIf(img -> img.getId().equals(imageId));
        productRepository.save(product);
        return ApiResponse.success("Image deleted.");
    }

    @Override
    public ApiResponse<String> bulkImportFromCsv(MultipartFile csvFile) {
        int imported = 0;
        int failed = 0;
        try (CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader()
                .parse(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {

            for (var record : parser) {
                try {
                    // Basic CSV columns: name,sku,categoryId,metalType,goldPurity,weightGrams,makingCharges,stockQty
                    String sku = record.get("sku").trim().toUpperCase();
                    if (productRepository.existsBySku(sku)) { failed++; continue; }

                    Long categoryId = Long.parseLong(record.get("categoryId").trim());
                    Category category = categoryRepository.findById(categoryId).orElse(null);
                    if (category == null) { failed++; continue; }

                    Product product = Product.builder()
                            .name(record.get("name").trim())
                            .sku(sku)
                            .category(category)
                            .metalType(record.get("metalType").trim())
                            .weightGrams(new BigDecimal(record.get("weightGrams").trim()))
                            .makingCharges(new BigDecimal(record.get("makingCharges").trim()))
                            .makingChargesType("PER_GRAM")
                            .stoneCharges(BigDecimal.ZERO)
                            .gstPercentage(new BigDecimal("3.00"))
                            .isActive(true)
                            .isBisHallmarked(true)
                            .slug(generateProductSlug(record.get("name").trim(), sku))
                            .build();

                    productRepository.save(product);

                    Inventory inv = Inventory.builder()
                            .product(product)
                            .quantity(Integer.parseInt(record.get("stockQty").trim()))
                            .lowStockThreshold(2)
                            .build();
                    inventoryRepository.save(inv);
                    imported++;
                } catch (Exception e) {
                    log.warn("CSV import failed for row {}: {}", parser.getCurrentLineNumber(), e.getMessage());
                    failed++;
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Failed to parse CSV file: " + e.getMessage());
        }
        return ApiResponse.success(String.format("Import complete. Imported: %d, Failed: %d", imported, failed));
    }

    // ── Mapping Helpers ───────────────────────────────────────

    private ProductResponse toProductResponse(Product p, GoldPrice goldPrice) {
        PricingEngine.PriceBreakdown price = null;
        if (goldPrice != null && p.getWeightGrams() != null && p.getGoldPurity() != null) {
            try { price = pricingEngine.calculatePrice(p, goldPrice); } catch (Exception ignored) {}
        }

        String primaryImage = p.getImages().stream()
                .filter(ProductImage::isPrimary).findFirst()
                .or(() -> p.getImages().stream().findFirst())
                .map(ProductImage::getImageUrl).orElse(null);

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .slug(p.getSlug())
                .metalType(p.getMetalType())
                .goldPurity(p.getGoldPurity())
                .weightGrams(p.getWeightGrams())
                .productCategory(p.getProductCategory())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .primaryImageUrl(primaryImage)
                .finalPrice(price != null ? price.getFinalPrice() : null)
                .baseMetalValue(price != null ? price.getBaseMetalValue() : null)
                .makingCharges(price != null ? price.getMakingCharges() : null)
                .gstAmount(price != null ? price.getGstAmount() : null)
                .currentGoldRate(goldPrice != null ? goldPrice.getRate22K() : null)
                .isBisHallmarked(p.isBisHallmarked())
                .isFeatured(p.isFeatured())
                .isNewArrival(p.isNewArrival())
                .isBestSeller(p.isBestSeller())
                .isInStock(p.getInventory() != null && p.getInventory().isInStock())
                .stockQuantity(p.getInventory() != null ? p.getInventory().getQuantity() : 0)
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ProductDetailResponse toDetailResponse(Product p) {
        GoldPrice goldPrice = null;
        try { goldPrice = goldPriceService.getCurrentGoldPriceEntity(); } catch (Exception ignored) {}

        PricingEngine.PriceBreakdown price = null;
        if (goldPrice != null && p.getWeightGrams() != null && p.getGoldPurity() != null) {
            try { price = pricingEngine.calculatePrice(p, goldPrice); } catch (Exception ignored) {}
        }

        List<ProductResponse> related = List.of();
        if (p.getCategory() != null) {
            GoldPrice finalGp = goldPrice;
            related = productRepository.findRelatedProducts(p.getCategory().getId(), p.getId(), PageRequest.of(0, 6))
                    .stream().map(rp -> toProductResponse(rp, finalGp)).toList();
        }

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .slug(p.getSlug())
                .description(p.getDescription())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)
                .productCategory(p.getProductCategory())
                .metalType(p.getMetalType())
                .goldPurity(p.getGoldPurity())
                .goldPurityDisplay(p.getGoldPurity() != null ? p.getGoldPurity().getDisplayName() +
                        " (" + p.getGoldPurity().getPurityPercentage() + "% Pure Gold)" : null)
                .bisHallmarkCode(p.getGoldPurity() != null ? p.getGoldPurity().getBisCode() : null)
                .weightGrams(p.getWeightGrams())
                .currentGoldRatePerGram(price != null ? price.getGoldRatePerGram() : null)
                .baseMetalValue(price != null ? price.getBaseMetalValue() : null)
                .makingCharges(price != null ? price.getMakingCharges() : null)
                .makingChargesType(p.getMakingChargesType())
                .stoneCharges(price != null ? price.getStoneCharges() : null)
                .taxableValue(price != null ? price.getTaxableValue() : null)
                .gstPercentage(p.getGstPercentage())
                .gstAmount(price != null ? price.getGstAmount() : null)
                .finalPrice(price != null ? price.getFinalPrice() : null)
                .isBisHallmarked(p.isBisHallmarked())
                .bisHallmarkNumber(p.getBisHallmarkNumber())
                .occasion(p.getOccasion())
                .gender(p.getGender())
                .dimensions(p.getDimensions())
                .finish(p.getFinish())
                .images(p.getImages().stream().map(img -> ProductImageResponse.builder()
                        .id(img.getId()).imageUrl(img.getImageUrl())
                        .altText(img.getAltText()).isPrimary(img.isPrimary())
                        .sortOrder(img.getSortOrder()).build()).toList())
                .inventory(p.getInventory() != null ? InventoryResponse.builder()
                        .quantity(p.getInventory().getQuantity())
                        .reservedQuantity(p.getInventory().getReservedQuantity())
                        .availableQuantity(p.getInventory().getAvailableQuantity())
                        .lowStockThreshold(p.getInventory().getLowStockThreshold())
                        .isInStock(p.getInventory().isInStock())
                        .isLowStock(p.getInventory().isLowStock())
                        .lastUpdated(p.getInventory().getLastUpdated())
                        .build() : null)
                .isFeatured(p.isFeatured())
                .isNewArrival(p.isNewArrival())
                .isBestSeller(p.isBestSeller())
                .isActive(p.isActive())
                .metaTitle(p.getMetaTitle())
                .metaDescription(p.getMetaDescription())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .relatedProducts(related)
                .build();
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeLowerFilter(String value) {
        String normalized = normalizeFilter(value);
        return normalized != null ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private String generateProductSlug(String name, String sku) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        String skuPart = sku.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String candidate = base + "-" + skuPart;
        if (!productRepository.existsBySlug(candidate)) return candidate;
        return candidate + "-" + System.currentTimeMillis();
    }
}
