package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.UpdateInventoryRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.InventoryResponse;

public interface InventoryService {
    InventoryResponse getInventory(Long productId);
    ApiResponse<InventoryResponse> updateInventory(UpdateInventoryRequest request);
}
