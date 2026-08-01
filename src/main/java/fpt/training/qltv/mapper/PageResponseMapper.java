package fpt.training.qltv.mapper;

import fpt.training.qltv.dto.paginate.PageResponse;
import fpt.training.qltv.dto.paginate.PaginationMeta;
import org.springframework.data.domain.Page;

public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return new PageResponse<>();
        }
        PaginationMeta meta = new PaginationMeta(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        return new PageResponse<>(page.getContent(), meta);
    }

    public static <T> PageResponse<T> from(fpt.training.qltv.dto.response.PageResponse<T> page) {
        if (page == null) {
            return new PageResponse<>();
        }
        PaginationMeta meta = new PaginationMeta(
            page.getCurrentPage(),
            page.getPageSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        return new PageResponse<>(page.getContent(), meta);
    }
}
