package com.library.service;

import com.library.dto.BorrowRecordDTO;
import com.library.mapper.BorrowRecordMapper;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.Feedback;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowRecordService {

  private final BorrowRecordRepository borrowRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;
  private final BorrowRecordMapper borrowMapper;
  private final FeedbackService feedbackService;

  // --- 1. MƯỢN MỘT DANH SÁCH SÁCH ---
  @Transactional
  public List<BorrowRecordDTO> borrowMultipleBooks(Long userId, List<Long> bookIds) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId));

    List<BorrowRecord> savedRecords = new ArrayList<>();

    for (Long bookId : bookIds) {
      Book book = bookRepository.findById(bookId)
          .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách ID: " + bookId));

      // Kiểm tra tồn kho
      if (book.getAvailableCopies() <= 0) {
        throw new RuntimeException("Sách '" + book.getTitle() + "' đã hết trong kho!");
      }

      // Kiểm tra xem User có đang mượn cuốn này mà chưa trả không
      if (borrowRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, BorrowRecord.Status.BORROWING)) {
        throw new RuntimeException("Bạn đang mượn cuốn '" + book.getTitle() + "' và chưa trả!");
      }

      // Tạo bản ghi mượn mới
      BorrowRecord record = new BorrowRecord();
      record.setUser(user);
      record.setBook(book);
      record.setBorrowDate(LocalDate.now());
      record.setDueDate(LocalDate.now().plusDays(14)); // Mặc định mượn 14 ngày
      record.setStatus(BorrowRecord.Status.BORROWING);

      // Giảm số lượng trong kho
      book.setAvailableCopies(book.getAvailableCopies() - 1);
      bookRepository.save(book);

      savedRecords.add(borrowRepository.save(record));
      feedbackService.recordEvent(userId, bookId, Feedback.EventType.BORROW);
    }

    return savedRecords.stream()
        .map(borrowMapper::toDTO)
        .collect(Collectors.toList());
  }

  // --- 2. LẤY DANH SÁCH CÁC PHIẾU MƯỢN ĐÃ QUÁ HẠN ---
  public List<BorrowRecordDTO> getOverdueRecords() {
    LocalDate today = LocalDate.now();
    // Tìm các phiếu đang mượn (BORROWING) mà dueDate đã qua so với hôm nay
    return borrowRepository.findAll().stream()
        .filter(r -> r.getStatus() == BorrowRecord.Status.BORROWING && r.getDueDate().isBefore(today))
        .map(borrowMapper::toDTO)
        .collect(Collectors.toList());
  }

  // --- 3. HÀM CẬP NHẬT TRẠNG THÁI QUÁ HẠN (CHẠY TỰ ĐỘNG) ---
  @Transactional
  public void updateOverdueStatus() {
    LocalDate today = LocalDate.now();
    List<BorrowRecord> overdueBooks = borrowRepository.findAll().stream()
        .filter(r -> r.getStatus() == BorrowRecord.Status.BORROWING && r.getDueDate().isBefore(today))
        .toList();

    overdueBooks.forEach(r -> r.setStatus(BorrowRecord.Status.OVERDUE));
    borrowRepository.saveAll(overdueBooks);
  }

  // --- 4. TRẢ SÁCH ---
  @Transactional
  public BorrowRecordDTO returnBook(Long recordId) {
      BorrowRecord record = borrowRepository.findById(recordId)
          .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiếu mượn!"));

      if (record.getStatus() == BorrowRecord.Status.RETURNED) {
          throw new RuntimeException("Sách này đã được trả trước đó.");
      }

      record.setStatus(BorrowRecord.Status.RETURNED);
      record.setReturnDate(LocalDate.now());

      Book book = record.getBook();
      book.setAvailableCopies(book.getAvailableCopies() + 1);
      bookRepository.save(book);

      BorrowRecord saved = borrowRepository.save(record);

      // Ghi feedback RETURN trước khi return
      feedbackService.recordEvent(
          record.getUser().getId(),
          record.getBook().getId(),
          Feedback.EventType.RETURN
      );

      return borrowMapper.toDTO(saved);
  }

  public List<BorrowRecordDTO> getUserBorrowHistory(Long userId) {
    if (!userRepository.existsById(userId)) {
        throw new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId);
    }
    return borrowRepository.findByUserId(userId).stream()
        .map(borrowMapper::toDTO)
        .collect(Collectors.toList());
  }
}