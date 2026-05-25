package com.library.scheduler;

import com.library.model.BorrowRecord;
import com.library.repository.BorrowRecordRepository;
import com.library.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BorrowReminderScheduler {

  private final BorrowRecordRepository borrowRecordRepository;
  private final EmailNotificationService emailService;

  // Chạy vào 8:00 sáng mỗi ngày
  @Scheduled(cron = "0 0 8 * * ?")
    @Transactional(readOnly = true)
    public void sendReminders() {
        log.info("Bắt đầu chạy Job quét hạn trả sách...");
        
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        // --- 1. NHẮC NHỞ TRƯỚC 3 NGÀY ---
        // Bạn cần viết thêm hàm findByStatusAndDueDate trong BorrowRecordRepository
        List<BorrowRecord> nearDueRecords = borrowRecordRepository.findByStatusAndDueDate(
                BorrowRecord.Status.BORROWING, threeDaysLater);

        for (BorrowRecord record : nearDueRecords) {
            String subject = "NHẮC NHỞ: Sách sắp đến hạn trả";
            String content = String.format(
                "Chào %s,\n\nCuốn sách '%s' của bạn sẽ hết hạn mượn vào ngày %s (3 ngày nữa).\n" +
                "Vui lòng sắp xếp thời gian trả sách đúng hạn để tránh phí phạt nhé!\n\nTrân trọng,",
                record.getUser().getFullName(), record.getBook().getTitle(), record.getDueDate()
            );
            emailService.sendEmail(record.getUser().getEmail(), subject, content);
        }

        // --- 2. THÔNG BÁO QUÁ HẠN (Vừa mới bước sang quá hạn) ---
        // Tìm những sách ĐANG MƯỢN nhưng dueDate là ngày hôm qua (tức là hnay đã thành quá hạn)
        LocalDate yesterday = today.minusDays(1);
        List<LocalDate> pastDates = ... // Tùy logic, thường người ta lấy những record có dueDate < today
        
        List<BorrowRecord> overdueRecords = borrowRecordRepository.findByStatusAndDueDateLessThan(
                BorrowRecord.Status.BORROWING, today);

        for (BorrowRecord record : overdueRecords) {
            String subject = "CẢNH BÁO: Sách đã quá hạn trả!";
            String content = String.format(
                "Chào %s,\n\nCuốn sách '%s' của bạn đã quá hạn trả (Hạn chót: %s).\n" +
                "Vui lòng mang sách đến trả ngay lập tức. Phí phạt có thể được áp dụng.\n\nTrân trọng,",
                record.getUser().getFullName(), record.getBook().getTitle(), record.getDueDate()
            );
            emailService.sendEmail(record.getUser().getEmail(), subject, content);
            
            // Lưu ý: Bạn có thể kết hợp gọi hàm update status sang OVERDUE ở đây luôn
        }
        
        log.info("Hoàn tất Job nhắc nhở.");
    }
}