package com.rajlaxmi.jewellers.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ================================================================
 * ApiResponse<T> — Universal API Response Wrapper
 * ================================================================
 * Every controller method returns ApiResponse<T> so the frontend
 * always gets a consistent JSON structure:
 *
 * SUCCESS:
 * {
 *   "success": true,
 *   "message": "Product fetched successfully",
 *   "data": { ...product object... },
 *   "timestamp": "2025-06-01T10:30:00"
 * }
 *
 * ERROR:
 * {
 *   "success": false,
 *   "message": "Product not found",
 *   "data": null,
 *   "timestamp": "2025-06-01T10:30:00"
 * }
 *
 * WHY @JsonInclude(NON_NULL)?
 *   Null fields (like 'data' on error) are excluded from JSON output.
 *   Keeps responses clean and avoids "data": null clutter.
 *
 * Generic type T: allows returning any payload type while
 * maintaining consistent wrapper structure. Frontend always
 * reads response.data regardless of which endpoint was called.
 * ================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Static Factory Methods ────────────────────────────────

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
