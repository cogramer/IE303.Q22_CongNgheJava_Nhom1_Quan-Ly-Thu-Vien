package com.library.controller.web;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.library.dto.BorrowRecordDTO;
import com.library.model.BorrowRecord;
import com.library.model.Reservation;
import com.library.repository.UserRepository;
import com.library.service.BookService;
import com.library.service.BorrowRecordService;
import com.library.service.RecommendService;
import com.library.service.ReservationService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reader")
public class WebReaderController {

  private final BookService bookService;
  private final ReservationService reservationService;
  private final RecommendService recommendService;
  private final UserRepository userRepository;
  private final BorrowRecordService borrowRecordService;

  // Helper: Lấy ID từ UserDetails
  private Long getUserId(UserDetails userDetails) {
    return userRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new EntityNotFoundException("User không tồn tại")).getId();
  }

  @GetMapping("/home")
  public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    Long userId = getUserId(userDetails);
    model.addAttribute("recommendations", recommendService.recommendBooks(userId));
    model.addAttribute("featuredBooks", bookService.getFeaturedBooks());
    model.addAttribute("newBooks", bookService.getNewBooks());

    // SỬA LẠI: Trả về đúng thư mục reader/home
    return "home";
  }

  @GetMapping("/books")
  public String books(@RequestParam(required = false) String keyword,
      @AuthenticationPrincipal UserDetails userDetails, Model model) {
    Long userId = getUserId(userDetails);

    // Giữ nguyên đoạn fix lỗi DTO Stream rất tốt của bạn
    List<Long> pendingBookIds = reservationService.getUserReservations(userId).stream()
        .filter(r -> Reservation.Status.PENDING.name().equals(r.getStatus()))
        .map(r -> r.getBookId())
        .collect(Collectors.toList());

    if (keyword != null && !keyword.isEmpty()) {
      model.addAttribute("books", bookService.searchBooks(keyword));
      model.addAttribute("keyword", keyword);
    } else {
      model.addAttribute("books", bookService.getAllBooks());
    }

    model.addAttribute("featuredBooks", bookService.getFeaturedBooks());
    model.addAttribute("pendingBookIds", pendingBookIds);

    return "reader/books";
  }

  // TÍNH NĂNG MỚI: Bổ sung trang Xem Chi Tiết 1 cuốn sách
  @GetMapping("/books/{id}")
  public String bookDetail(@PathVariable Long id, Model model) {
    // Giả sử bạn có hàm getBookById trả về BookDTO
    model.addAttribute("book", bookService.getBookById(id));
    return "reader/book-detail"; // Bạn sẽ cần tạo thêm file reader/book-detai.html
  }

  @GetMapping("/borrow")
  public String borrow(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    Long userId = getUserId(userDetails);

    // SỬA LỖI: Đồng bộ cách so sánh String và Enum giống như ở hàm books()
    List<BorrowRecordDTO> activeLoans = borrowRecordService.getUserBorrowHistory(userId).stream()
        .filter(r -> BorrowRecord.Status.BORROWING.name().equals(r.getStatus()) ||
            BorrowRecord.Status.OVERDUE.name().equals(r.getStatus()))
        .collect(Collectors.toList());

    model.addAttribute("loans", activeLoans);
    return "reader/borrow";
  }

  @GetMapping("/history")
  public String borrowHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    Long userId = getUserId(userDetails);
    
    // Lấy tất cả records
    List<BorrowRecordDTO> allRecords = borrowRecordService.getUserBorrowHistory(userId);
    
    // Lọc chỉ RETURNED records
    List<BorrowRecordDTO> returnedRecords = allRecords.stream()
        .filter(r -> BorrowRecord.Status.RETURNED.name().equals(r.getStatus()))
        .collect(Collectors.toList());
    
    // Tính thống kê
    long totalReturned = returnedRecords.size();
    long lateCount = allRecords.stream()
        .filter(r -> BorrowRecord.Status.OVERDUE.name().equals(r.getStatus()))
        .count();
    
    // Phí phạt (tạm thời đặt là 0 - có thể tính từ số ngày trễ hạn sau)
    double lateFee = lateCount * 5000; // 5000đ/ngày trễ (ví dụ)
    
    model.addAttribute("records", returnedRecords);
    model.addAttribute("totalReturned", totalReturned);
    model.addAttribute("lateCount", lateCount);
    model.addAttribute("lateFee", lateFee);
    
    return "reader/history";
  }

  @GetMapping("/reservations")
  public String reservations(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    Long userId = getUserId(userDetails);
    model.addAttribute("reservations", reservationService.getUserReservations(userId));
    return "reader/reservations";
  }

  @GetMapping("/recommendations")
  public String recommendations(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    Long userId = getUserId(userDetails);
    model.addAttribute("books", recommendService.recommendBooks(userId));
    return "reader/recommendations";
  }
}