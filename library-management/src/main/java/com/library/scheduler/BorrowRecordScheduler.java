package com.library.scheduler;

import com.library.service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BorrowRecordScheduler {

  private final BorrowRecordService borrowService;

  // Chạy tự động vào lúc 00:00:00 mỗi ngày
  @Scheduled(cron = "0 0 0 * * ?")
  public void scheduleOverdueCheck() {
    borrowService.updateOverdueStatus();
    System.out.println("Cronjob: Đã quét và cập nhật trạng thái sách quá hạn.");
  }
}