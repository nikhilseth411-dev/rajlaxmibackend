package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.AddressRequest;
import com.rajlaxmi.jewellers.dto.response.AddressResponse;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Manage delivery addresses")
@SecurityRequirement(name = "BearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Get all saved addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getUserAddresses(user.getId())));
    }

    @PostMapping
    @Operation(summary = "Add a new delivery address")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.addAddress(user.getId(), request));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal User user,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(user.getId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            @AuthenticationPrincipal User user,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.deleteAddress(user.getId(), addressId));
    }

    @PutMapping("/{addressId}/default")
    @Operation(summary = "Set address as default delivery address")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @AuthenticationPrincipal User user,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(user.getId(), addressId));
    }
}
