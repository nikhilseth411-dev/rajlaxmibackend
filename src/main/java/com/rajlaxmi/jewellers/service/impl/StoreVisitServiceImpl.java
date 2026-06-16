package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.BookStoreVisitRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PagedResponse;
import com.rajlaxmi.jewellers.dto.response.StoreVisitResponse;
import com.rajlaxmi.jewellers.entity.StoreVisit;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.StoreVisitRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.StoreVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreVisitServiceImpl implements StoreVisitService {

    private final StoreVisitRepository storeVisitRepository;
    private final UserRepository userRepository;

    // Available time slots for Wazirganj store
    private static final List<String> VALID_SLOTS = List.of(
            "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"
    );

    @Override
    public ApiResponse<StoreVisitResponse> bookVisit(Long userId, BookStoreVisitRequest request) {
        if (!VALID_SLOTS.contains(request.getTimeSlot())) {
            throw new BusinessException("Invalid time slot. Available: " + String.join(", ", VALID_SLOTS));
        }

        // Check for same-date slot availability (max 3 visits per slot)
        long existingForSlot = storeVisitRepository
                .findByVisitDateAndStatus(request.getVisitDate(), "PENDING")
                .stream()
                .filter(v -> v.getTimeSlot().equals(request.getTimeSlot()))
                .count() +
                storeVisitRepository
                .findByVisitDateAndStatus(request.getVisitDate(), "CONFIRMED")
                .stream()
                .filter(v -> v.getTimeSlot().equals(request.getTimeSlot()))
                .count();

        if (existingForSlot >= 3) {
            throw new BusinessException("This time slot is fully booked. Please choose a different time.");
        }

        User user = userRepository.getReferenceById(userId);
        StoreVisit visit = StoreVisit.builder()
                .user(user)
                .visitDate(request.getVisitDate())
                .timeSlot(request.getTimeSlot())
                .status("PENDING")
                .purposeNote(request.getPurposeNote())
                .contactPhone(request.getContactPhone())
                .build();

        storeVisitRepository.save(visit);
        return ApiResponse.success(
                "Store visit booked for " + request.getVisitDate() + " at " + request.getTimeSlot() +
                ". Our team will confirm shortly on WhatsApp.",
                toResponse(visit));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StoreVisitResponse> getUserVisits(Long userId, int page, int size) {
        return PagedResponse.from(
                storeVisitRepository.findByUserIdOrderByCreatedAtDesc(userId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())),
                this::toResponse);
    }

    @Override
    public ApiResponse<String> cancelVisit(Long visitId, Long userId) {
        StoreVisit visit = storeVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Store visit", "id", visitId));

        if (!visit.getUser().getId().equals(userId)) throw new BusinessException("Access denied.");
        if ("CANCELLED".equals(visit.getStatus())) throw new BusinessException("Visit already cancelled.");

        visit.setStatus("CANCELLED");
        storeVisitRepository.save(visit);
        return ApiResponse.success("Visit on " + visit.getVisitDate() + " has been cancelled.");
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StoreVisitResponse> getPendingVisits(int page, int size) {
        return PagedResponse.from(
                storeVisitRepository.findByStatusOrderByVisitDateAsc("PENDING",
                        PageRequest.of(page, size)),
                this::toResponse);
    }

    @Override
    public ApiResponse<StoreVisitResponse> confirmVisit(Long visitId, String adminNote) {
        StoreVisit visit = storeVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Store visit", "id", visitId));
        visit.setStatus("CONFIRMED");
        visit.setAdminNote(adminNote);
        storeVisitRepository.save(visit);
        return ApiResponse.success("Visit confirmed.", toResponse(visit));
    }

    private StoreVisitResponse toResponse(StoreVisit v) {
        return StoreVisitResponse.builder()
                .id(v.getId()).visitDate(v.getVisitDate()).timeSlot(v.getTimeSlot())
                .status(v.getStatus()).purposeNote(v.getPurposeNote())
                .contactPhone(v.getContactPhone()).adminNote(v.getAdminNote())
                .createdAt(v.getCreatedAt()).build();
    }
}
