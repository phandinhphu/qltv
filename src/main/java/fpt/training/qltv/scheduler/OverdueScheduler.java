package fpt.training.qltv.scheduler;

import fpt.training.qltv.repository.BorrowRecordRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueScheduler {

    private final BorrowRecordRepository borrowRecordRepository;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void checkAndMarkOverdue() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = borrowRecordRepository.markOverdue(now);
            log.info("Đã đánh dấu {} bản ghi mượn quá hạn.", updatedCount);
            if (updatedCount > 0) {
                org.springframework.cache.Cache dashboardCache = cacheManager.getCache("dashboard");
                if (dashboardCache != null) {
                    dashboardCache.clear();
                    log.info("Đã xóa cache dashboard do có bản ghi quá hạn mới.");
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi đánh dấu các bản ghi mượn quá hạn: {}", e.getMessage(), e);
        }
    }
}
