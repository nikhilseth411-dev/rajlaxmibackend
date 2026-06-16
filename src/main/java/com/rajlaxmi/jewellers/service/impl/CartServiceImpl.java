package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.AddToCartRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CartItemResponse;
import com.rajlaxmi.jewellers.dto.response.CartResponse;
import com.rajlaxmi.jewellers.entity.Cart;
import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.entity.Product;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.CartRepository;
import com.rajlaxmi.jewellers.repository.ProductRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.CartService;
import com.rajlaxmi.jewellers.service.GoldPriceService;
import com.rajlaxmi.jewellers.util.PricingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final GoldPriceService goldPriceService;
    private final PricingEngine pricingEngine;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<Cart> cartItems = cartRepository.findByUserIdWithProducts(userId);
        GoldPrice goldPrice = goldPriceService.getCurrentGoldPriceEntity();
        return buildCartResponse(cartItems, goldPrice);
    }

    @Override
    public ApiResponse<CartResponse> addToCart(Long userId, AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.isActive()) throw new BusinessException("This product is no longer available.");

        if (product.getInventory() == null || !product.getInventory().isInStock()) {
            throw new BusinessException("'" + product.getName() + "' is currently out of stock.");
        }

        int available = product.getInventory().getAvailableQuantity();
        if (request.getQuantity() > available) {
            throw new BusinessException("Only " + available + " unit(s) available in stock.");
        }

        User user = userRepository.getReferenceById(userId);

        if (cartRepository.existsByUserIdAndProductId(userId, product.getId())) {
            // Update quantity
            Cart existing = cartRepository.findByUserIdAndProductId(userId, product.getId()).orElseThrow();
            int newQty = existing.getQuantity() + request.getQuantity();
            if (newQty > available) throw new BusinessException("Cannot add more than " + available + " unit(s).");
            existing.setQuantity(newQty);
            cartRepository.save(existing);
        } else {
            Cart cartItem = Cart.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartRepository.save(cartItem);
        }

        return ApiResponse.success("Added to cart.", getCart(userId));
    }

    @Override
    public ApiResponse<CartResponse> updateQuantity(Long userId, Long productId, int quantity) {
        Cart cart = cartRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            cartRepository.delete(cart);
        } else {
            int available = cart.getProduct().getInventory() != null
                    ? cart.getProduct().getInventory().getAvailableQuantity() : 0;
            if (quantity > available) throw new BusinessException("Only " + available + " unit(s) available.");
            cart.setQuantity(quantity);
            cartRepository.save(cart);
        }
        return ApiResponse.success("Cart updated.", getCart(userId));
    }

    @Override
    public ApiResponse<String> removeFromCart(Long userId, Long productId) {
        cartRepository.deleteByUserIdAndProductId(userId, productId);
        return ApiResponse.success("Item removed from cart.");
    }

    @Override
    public ApiResponse<String> clearCart(Long userId) {
        cartRepository.clearCartByUserId(userId);
        return ApiResponse.success("Cart cleared.");
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return (int) cartRepository.countByUserId(userId);
    }

    private CartResponse buildCartResponse(List<Cart> items, GoldPrice goldPrice) {
        List<CartItemResponse> itemResponses = items.stream().map(cart -> {
            Product p = cart.getProduct();
            PricingEngine.PriceBreakdown price = null;
            if (goldPrice != null && p.getWeightGrams() != null && p.getGoldPurity() != null) {
                try { price = pricingEngine.calculatePrice(p, goldPrice); } catch (Exception ignored) {}
            }
            BigDecimal unitPrice = price != null ? price.getFinalPrice() : BigDecimal.ZERO;
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cart.getQuantity()));

            String img = p.getImages().stream().filter(i -> i.isPrimary()).findFirst()
                    .or(() -> p.getImages().stream().findFirst())
                    .map(i -> i.getImageUrl()).orElse(null);

            return CartItemResponse.builder()
                    .cartItemId(cart.getId())
                    .productId(p.getId())
                    .productName(p.getName())
                    .productSlug(p.getSlug())
                    .primaryImageUrl(img)
                    .sku(p.getSku())
                    .metalType(p.getMetalType())
                    .goldPurity(p.getGoldPurity() != null ? p.getGoldPurity().getDisplayName() : null)
                    .weightGrams(p.getWeightGrams())
                    .quantity(cart.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .goldRateUsed(goldPrice != null ? goldPrice.getRate22K() : null)
                    .isInStock(p.getInventory() != null && p.getInventory().isInStock())
                    .availableStock(p.getInventory() != null ? p.getInventory().getAvailableQuantity() : 0)
                    .addedAt(cart.getAddedAt())
                    .build();
        }).toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gst = itemResponses.stream()
                .map(i -> i.getTotalPrice().multiply(new BigDecimal("0.03")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .subtotal(subtotal)
                .totalGst(gst)
                .grandTotal(subtotal)
                .currentGoldRate22K(goldPrice != null ? goldPrice.getRate22K() : null)
                .priceDisclaimer("Prices shown are based on live gold rates and may change at checkout.")
                .build();
    }
}
