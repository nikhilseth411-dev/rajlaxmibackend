package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.entity.Category;
import com.rajlaxmi.jewellers.entity.Product;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import com.rajlaxmi.jewellers.repository.CategoryRepository;
import com.rajlaxmi.jewellers.repository.InventoryRepository;
import com.rajlaxmi.jewellers.repository.ProductRepository;
import com.rajlaxmi.jewellers.service.GoldPriceService;
import com.rajlaxmi.jewellers.util.PricingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private GoldPriceService goldPriceService;
    @Mock private PricingEngine pricingEngine;

    @TempDir
    Path uploadRoot;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(
                productRepository,
                categoryRepository,
                inventoryRepository,
                goldPriceService,
                pricingEngine
        );
        ReflectionTestUtils.setField(service, "uploadDir", uploadRoot.toString());
    }

    @Test
    void resolvesSpecificCategoryFromSelectedDatabaseCategory() {
        Category bangles = Category.builder().name("Bangles").slug("gold-bangles").build();
        Category earrings = Category.builder().name("Earrings").slug("gold-earrings").build();

        assertThat(ProductServiceImpl.resolveProductCategory(
                bangles, ProductCategory.GOLD_JEWELLERY)).isEqualTo(ProductCategory.BANGLES);
        assertThat(ProductServiceImpl.resolveProductCategory(
                earrings, ProductCategory.GOLD_JEWELLERY)).isEqualTo(ProductCategory.EARRINGS);
    }

    @Test
    void normalizesCategoryFoldersWithoutAllowingPathSegments() {
        Category category = Category.builder()
                .id(14L)
                .name("../Gold Jewellery / Bridal Collection")
                .build();

        assertThat(ProductServiceImpl.normalizeCategoryFolder(category))
                .isEqualTo("gold-jewellery-bridal-collection")
                .doesNotContain("..", "/", "\\");
    }

    @Test
    void uploadsImageIntoSelectedCategoryFolderWithUuidName() throws Exception {
        Category bangles = Category.builder().id(3L).name("Bangles").slug("gold-bangles").build();
        Product product = Product.builder().id(21L).name("Classic Gold Bangles").category(bangles).build();
        MockMultipartFile image = new MockMultipartFile(
                "file", "../../../unsafe-name.jpg", "image/jpeg", "image-data".getBytes()
        );
        when(productRepository.findById(21L)).thenReturn(Optional.of(product));

        ApiResponse<String> response = service.uploadProductImage(21L, image, true);

        assertThat(response.getData()).matches("uploads/products/bangles/[0-9a-f-]+\\.jpg");
        Path storedFile = uploadRoot.resolve(response.getData().replace("uploads/", ""));
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(storedFile.normalize().startsWith(uploadRoot.resolve("products/bangles"))).isTrue();
        verify(productRepository).save(product);
    }

    @Test
    void softDeletesProductWithoutRemovingOrderHistoryReference() {
        Product product = Product.builder().id(31L).name("Archived Necklace").isActive(true).build();
        when(productRepository.findById(31L)).thenReturn(Optional.of(product));

        service.deleteProduct(31L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }
}
