package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.UpdateInventoryRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.InventoryResponse;
import com.rajlaxmi.jewellers.entity.Inventory;
import com.rajlaxmi.jewellers.entity.Product;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.InventoryRepository;
import com.rajlaxmi.jewellers.repository.ProductRepository;
import com.rajlaxmi.jewellers.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));
        return toResponse(inventory);
    }

    @Override
    public ApiResponse<InventoryResponse> updateInventory(UpdateInventoryRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseGet(() -> Inventory.builder()
                        .product(product)
                        .quantity(0)
                        .reservedQuantity(0)
                        .lowStockThreshold(request.getLowStockThreshold())
                        .build());

        if (request.getQuantity() < inventory.getReservedQuantity()) {
            throw new BusinessException("Quantity cannot be less than reserved quantity (" +
                    inventory.getReservedQuantity() + ").");
        }

        inventory.setQuantity(request.getQuantity());
        inventory.setLowStockThreshold(request.getLowStockThreshold());
        Inventory saved = inventoryRepository.save(inventory);

        log.info("Inventory updated for product {} to quantity {}. Reason: {}",
                product.getSku(), request.getQuantity(), request.getReason());

        return ApiResponse.success("Inventory updated successfully.", toResponse(saved));
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .sku(inventory.getProduct().getSku())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .isInStock(inventory.isInStock())
                .isLowStock(inventory.isLowStock())
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }
}
