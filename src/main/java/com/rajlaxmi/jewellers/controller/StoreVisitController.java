package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.BookStoreVisitRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PagedResponse;
import com.rajlaxmi.jewellers.dto.response.StoreVisitResponse;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.service.StoreVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store-visits")
@RequiredArgsConstructor
@Tag(name = "Store Visits", description = "Book and manage in-store appointments")
@SecurityRequirement(name = "BearerAuth")
public class StoreVisitController {

    private final StoreVisitService storeVisitService;

    @PostMapping("/book")
    @Operation(summary = "Book a store visit appointment")
    public ResponseEntity<ApiResponse<StoreVisitResponse>> book(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookStoreVisitRequest request) {
        return ResponseEntity.ok(storeVisitService.bookVisit(user.getId(), request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my store visit history")
    public ResponseEntity<ApiResponse<PagedResponse<StoreVisitResponse>>> getMyVisits(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(storeVisitService.getUserVisits(user.getId(), page, size)));
    }

    @PutMapping("/{visitId}/cancel")
    @Operation(summary = "Cancel a store visit booking")
    public ResponseEntity<ApiResponse<String>> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable Long visitId) {
        return ResponseEntity.ok(storeVisitService.cancelVisit(visitId, user.getId()));
    }

    // ── Admin ─────────────────────────────────────────────────

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get pending visit bookings")
    public ResponseEntity<ApiResponse<PagedResponse<StoreVisitResponse>>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(storeVisitService.getPendingVisits(page, size)));
    }

    @PutMapping("/admin/{visitId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Confirm a store visit")
    public ResponseEntity<ApiResponse<StoreVisitResponse>> confirm(
            @PathVariable Long visitId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(storeVisitService.confirmVisit(visitId, note));
    }

    @GetMapping("/available-slots")
    @Operation(summary = "Get available time slots for a given date")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableSlots() {
        List<String> slots = List.of(
                "10:00-11:00", "11:00-12:00", "12:00-13:00",
                "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"
        );
        return ResponseEntity.ok(ApiResponse.success("Store timings: Mon-Sat, 10:00 AM - 6:00 PM", slots));
    }
}
