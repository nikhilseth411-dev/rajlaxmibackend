package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.CreateCategoryRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CategoryResponse;
import com.rajlaxmi.jewellers.entity.Category;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.DuplicateResourceException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.CategoryRepository;
import com.rajlaxmi.jewellers.repository.ProductRepository;
import com.rajlaxmi.jewellers.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Cacheable(value = "categories", key = "'root'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Cacheable(value = "categories", key = "'tree'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        return categoryRepository.findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toTreeResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Cacheable(value = "categories", key = "'children:' + #parentId")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(Long parentId) {
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Category", "id", parentId);
        }
        return categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(parentId)
                .stream().map(this::toResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Cacheable(value = "categories", key = "'slug:' + #slug")
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
    }

    @Override
    @Cacheable(value = "categories", key = "'id:' + #id")
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public ApiResponse<CategoryResponse> createCategory(CreateCategoryRequest request) {
        String slug = generateSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .parent(parent)
                .sortOrder(request.getSortOrder())
                .isActive(request.isActive())
                .build();

        categoryRepository.save(category);
        return ApiResponse.success("Category created successfully.", toResponse(category));
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public ApiResponse<CategoryResponse> updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (request.getName() != null) {
            String newName = request.getName().trim();
            String newSlug = generateSlug(newName);
            categoryRepository.findBySlug(newSlug)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("Category", "name", newName);
                    });
            category.setName(newName);
            category.setSlug(newSlug);
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        if (request.getSortOrder() >= 0) category.setSortOrder(request.getSortOrder());
        category.setParent(resolveParentForUpdate(id, request.getParentId()));
        category.setActive(request.isActive());

        categoryRepository.save(category);
        return ApiResponse.success("Category updated.", toResponse(category));
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public ApiResponse<String> deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        category.setActive(false);
        categoryRepository.save(category);
        return ApiResponse.success("Category deactivated.");
    }

    // ── Helpers ───────────────────────────────────────────────

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .sortOrder(c.getSortOrder())
                .isActive(c.isActive())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .productCount(productRepository.countByCategoryIdAndIsActiveTrue(c.getId()))
                .build();
    }

    private CategoryResponse toTreeResponse(Category c) {
        CategoryResponse response = toResponse(c);
        response.setChildren(c.getChildren().stream()
                .filter(Category::isActive)
                .sorted(java.util.Comparator.comparingInt(Category::getSortOrder))
                .map(this::toTreeResponse)
                .toList());
        return response;
    }

    /**
     * Generates URL-safe slug from category name.
     * "Gold Jewellery" → "gold-jewellery"
     * "मंगलसूत्र" → "mangalsutra" (normalized)
     */
    public static String generateSlug(String name) {
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFKC);
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .trim();

        if (slug.isBlank()) {
            throw new BusinessException("Category name must contain letters or numbers.");
        }

        return slug;
    }

    private Category resolveParentForUpdate(Long categoryId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        if (parentId.equals(categoryId)) {
            throw new BusinessException("A category cannot be its own parent.");
        }

        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", parentId));

        if (isDescendantOf(parent, categoryId)) {
            throw new BusinessException("A category cannot be moved under one of its own child categories.");
        }

        return parent;
    }

    private boolean isDescendantOf(Category category, Long ancestorId) {
        Category current = category;
        while (current != null) {
            if (ancestorId.equals(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
