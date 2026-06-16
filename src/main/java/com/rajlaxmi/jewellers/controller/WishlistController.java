package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CartResponse;
import com.rajlaxmi.jewellers.dto.response.WishlistResponse;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Save favourite jewellery products")
@SecurityRequirement(name = "BearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get user's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlist(user.getId())));
    }

    @PostMapping("/add/{productId}")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<String>> add(
            @AuthenticationPrincipal User user, @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.addToWishlist(user.getId(), productId));
    }

    @DeleteMapping("/remove/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<String>> remove(
            @AuthenticationPrincipal User user, @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.removeFromWishlist(user.getId(), productId));
    }

    @PostMapping("/move-to-cart/{productId}")
    @Operation(summary = "Move product from wishlist to cart")
    public ResponseEntity<ApiResponse<CartResponse>> moveToCart(
            @AuthenticationPrincipal User user, @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.moveToCart(user.getId(), productId));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Boolean>> checkWishlist(
            @AuthenticationPrincipal User user, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.isInWishlist(user.getId(), productId)));
    }
}
