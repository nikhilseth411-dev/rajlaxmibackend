package com.rajlaxmi.jewellers.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * PagedResponse<T> — Wraps Spring Page results for API responses.
 *
 * Spring's Page object has too many internal fields for direct serialization.
 * This DTO exposes only what the frontend needs:
 *   - content: the list of items for this page
 *   - page/size/totalElements/totalPages: for rendering pagination controls
 *   - isFirst/isLast: for disabling prev/next buttons
 *
 * Usage in service:
 *   Page<Product> page = productRepository.findAll(pageable);
 *   return PagedResponse.from(page, productMapper::toResponse);
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Convenience factory that maps a Spring Page<E> to PagedResponse<T>
     * using a provided mapper function.
     *
     * @param page Spring Page result from repository
     * @param mapper function to convert entity E to DTO T
     */
    public static <E, T> PagedResponse<T> from(Page<E> page,
                                                java.util.function.Function<E, T> mapper) {
        return PagedResponse.<T>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
