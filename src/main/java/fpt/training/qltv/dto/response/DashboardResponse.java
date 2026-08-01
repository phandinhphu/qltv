package fpt.training.qltv.dto.response;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardResponse {

    private Long totalBooks;
    private Long totalUsers;
    private Long totalActiveBorrows;
    private Long totalOverdue;
    private List<BookResponse> topBorrowedBooks;
    private Map<String, Long> borrowCountByMonth;
}
