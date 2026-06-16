package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.AddToCartRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CartResponse;
import com.rajlaxmi.jewellers.dto.response.ProductResponse;
import com.rajlaxmi.jewellers.dto.response.WishlistResponse;
import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.entity.Product;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.entity.Wishlist;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.ProductRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.repository.WishlistRepository;
import com.rajlaxmi.jewellers.service.CartService;
import com.rajlaxmi.jewellers.service.GoldPriceService;
import com.rajlaxmi.jewellers.service.WishlistService;
import com.rajlaxmi.jewellers.util.PricingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final GoldPriceService goldPriceService;
    private final PricingEngine pricingEngine;
    private final CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdWithProducts(userId);
        GoldPrice goldPrice = goldPriceService.getCurrentGoldPriceEntity();

        // Explicit List<ProductResponse> type avoids Java type-inference failure in complex lambda
        List<ProductResponse> products = wishlists.stream()
                .map((Wishlist w) -> buildProductResponse(w.getProduct(), goldPrice))
                .toList();

        return WishlistResponse.builder()
                .products(products)
                .totalItems(products.size())
                .build();
    }

    private ProductResponse buildProductResponse(Product p, GoldPrice goldPrice) {
        PricingEngine.PriceBreakdown priceBreakdown = null;
        if (goldPrice != null && p.getWeightGrams() != null && p.getGoldPurity() != null) {
            try {
                priceBreakdown = pricingEngine.calculatePrice(p, goldPrice);
            } catch (Exception ignored) {}
        }

        String img = p.getImages().stream()
                .filter(i -> i.isPrimary()).findFirst()
                .or(() -> p.getImages().stream().findFirst())
                .map(i -> i.getImageUrl())
                .orElse(null);

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .slug(p.getSlug())
                .metalType(p.getMetalType())
                .goldPurity(p.getGoldPurity())
                .weightGrams(p.getWeightGrams())
                .primaryImageUrl(img)
                .finalPrice(priceBreakdown != null ? priceBreakdown.getFinalPrice() : null)
                .isBisHallmarked(p.isBisHallmarked())
                .isInStock(p.getInventory() != null && p.getInventory().isInStock())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build();
    }

    @Override
    public ApiResponse<String> addToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return ApiResponse.success("Product is already in your wishlist.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        User user = userRepository.getReferenceById(userId);
        wishlistRepository.save(Wishlist.builder().user(user).product(product).build());
        return ApiResponse.success("Added to wishlist.");
    }

    @Override
    public ApiResponse<String> removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        return ApiResponse.success("Removed from wishlist.");
    }

    @Override
    public ApiResponse<CartResponse> moveToCart(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        AddToCartRequest req = new AddToCartRequest();
        req.setProductId(productId);
        req.setQuantity(1);
        return cartService.addToCart(userId, req);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
}
