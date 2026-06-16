package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.BookStoreVisitRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PagedResponse;
import com.rajlaxmi.jewellers.dto.response.StoreVisitResponse;

public interface StoreVisitService {
    ApiResponse<StoreVisitResponse> bookVisit(Long userId, BookStoreVisitRequest request);
    PagedResponse<StoreVisitResponse> getUserVisits(Long userId, int page, int size);
    ApiResponse<String> cancelVisit(Long visitId, Long userId);
    // Admin
    PagedResponse<StoreVisitResponse> getPendingVisits(int page, int size);
    ApiResponse<StoreVisitResponse> confirmVisit(Long visitId, String adminNote);
}
