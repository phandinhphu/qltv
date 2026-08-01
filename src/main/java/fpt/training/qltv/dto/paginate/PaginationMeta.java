package fpt.training.qltv.dto.paginate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationMeta {
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
}
