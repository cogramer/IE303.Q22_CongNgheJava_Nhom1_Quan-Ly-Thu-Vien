package com.library.dto;

import com.library.model.BorrowRecord.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class BorrowRecordDTO {

  private Long id;

  @NotNull(message = "ID sách không được để trống")
  private Long bookId;

  private String bookTitle; // Thường chỉ dùng để trả về (Response), không cần validate khi nhận vào

  @NotNull(message = "ID người dùng không được để trống")
  private Long userId;

  private String userName;

  // Lưu ý: Mình đổi sang LocalDate cho khớp với Entity bạn đã sửa ở trên
  private LocalDate borrowDate;

  @FutureOrPresent(message = "Hạn trả sách không được là ngày trong quá khứ")
  private LocalDate dueDate;

  private LocalDate returnDate;

  private Status status;
}