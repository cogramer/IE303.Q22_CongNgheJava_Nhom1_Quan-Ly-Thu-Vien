package com.library.controller.web;

import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.library.dto.BookDTO;
import com.library.dto.UserDTO;
import com.library.service.BookService;
import com.library.service.BorrowRecordService;
import com.library.service.CategoryService;
import com.library.service.ReservationService;
import com.library.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/librarian")
public class WebLibrarianController {

  private final BookService bookService;
  private final CategoryService categoryService;
  private final BorrowRecordService borrowService;
  private final ReservationService reservationService;
  private final UserService userService;

  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    model.addAttribute("totalBooks", bookService.getAllBooks().size());
    model.addAttribute("totalUsers", userService.getAllUsers().size());
    model.addAttribute("overdueList", borrowService.getOverdueRecords());
    model.addAttribute("pendingReservations", reservationService.getPendingReservations());
    model.addAttribute("recentActivity", borrowService.getRecentActivity());
    model.addAttribute("topBorrowedBooks", borrowService.getTopBorrowedBooks());
    model.addAttribute("newBooks", bookService.getNewBooks());
    int currentYear = Year.now().getValue();
    model.addAttribute("borrowByMonth", borrowService.getBorrowCountByMonth(currentYear));
    model.addAttribute("borrowingRate", bookService.getBorrowingRate());
    model.addAttribute("topCategories", categoryService.getCategoryHotStats());
    return "librarian/dashboard";
  }

  @GetMapping("/books")
  public String books(Model model) {
    model.addAttribute("books", bookService.getAllBooks());
    model.addAttribute("categories", categoryService.getAllCategories());
    return "librarian/books";
  }

  @GetMapping("/users")
  public String users(@RequestParam(required = false) String keyword, Model model) {
    if (keyword != null && !keyword.isBlank()) {
      model.addAttribute("users", userService.searchUsersByName(keyword));
      model.addAttribute("keyword", keyword);
    } else {
      model.addAttribute("users", userService.getAllUsers());
    }
    return "librarian/users";
  }

  @GetMapping("/loans")
  public String loans(@RequestParam(required = false) String keyword, Model model) {
    model.addAttribute("loans", borrowService.getAllBorrowRecords());
    model.addAttribute("keyword", keyword);
    model.addAttribute("overdueList", borrowService.getOverdueRecords());
    return "librarian/loans";
  }

  @GetMapping("/reservations")
  public String reservations(Model model) {
    model.addAttribute("reservations", reservationService.getAllReservations());
    return "librarian/reservations";
  }

  @GetMapping("/reports")
  public String reports(Model model) {
    model.addAttribute("totalBooks", bookService.getAllBooks().size());
    model.addAttribute("loans", borrowService.getAllBorrowRecords());
    model.addAttribute("topBorrowedBooks", borrowService.getTopBorrowedBooks());
    model.addAttribute("borrowByMonth", borrowService.getBorrowCountByMonth(2026));
    model.addAttribute("topCategories", categoryService.getCategoryHotStats());
    model.addAttribute("overdueList", borrowService.getOverdueRecords());
    return "librarian/reports";
  }

  // Xử lý Thêm mới và Cập nhật sách
  @PostMapping("/books/save")
  public String saveBook(@ModelAttribute BookDTO bookDTO) {
    // Gọi service lưu sách (Service của bạn đã handle cả logic Create lẫn Update
    // dựa trên ID)
    bookService.saveBook(bookDTO);

    // Lưu thành công thì load lại trang quản lý sách
    return "redirect:/librarian/books";
  }

  // Xử lý Xóa sách
  @PostMapping("/books/delete/{id}")
  public String deleteBook(@PathVariable Long id) {
    bookService.deleteBook(id);

    // Xóa thành công thì load lại trang quản lý sách
    return "redirect:/librarian/books";
  }

  // Xử lý Duyệt (Fulfill) đặt trước sách
  @PostMapping("/reservations/fulfill/{id}")
  public String fulfillReservation(@PathVariable Long id) {
    // Gọi service để xử lý logic duyệt đặt trước (chuyển trạng thái FULFILLED, tự
    // động tạo phiếu mượn...)
    // Lưu ý: Đảm bảo tên hàm trong reservationService khớp với hàm thực tế của bạn
    // (ví dụ: fulfillReservation)
    reservationService.fulfillReservation(id);

    // Sau khi duyệt xong thì load lại trang danh sách đặt giữ
    return "redirect:/librarian/reservations";
  }

  // Xử lý xác nhận trả sách (1 hoặc nhiều cuốn)
  @PostMapping("/loans/return")
  public String returnLoans(@RequestParam("loanIds") List<Long> loanIds) {
    // Gọi service xử lý trả sách theo list ID
    borrowService.returnMultipleBooks(loanIds);
    return "redirect:/librarian/loans";
  }

  // Xử lý Cập nhật người dùng
  @PostMapping("/users/save")
  public String saveUser(@ModelAttribute UserDTO userDTO) {
    // Cần lấy ID từ DTO truyền vào hàm updateUser của UserService
    if (userDTO.getId() != null) {
      userService.updateUser(userDTO.getId(), userDTO);

      // Nếu bạn cho phép đổi cả Role từ form này, cần gọi thêm:
      // userService.updateUserRole(userDTO.getId(), userDTO.getRole());
    }
    return "redirect:/librarian/users";
  }

  // Xử lý Xóa người dùng
  @PostMapping("/users/delete")
  public String deleteUser(@RequestParam("id") Long id) {
    // Gọi hàm deleteUser đã có sẵn trong UserService
    userService.deleteUser(id);
    return "redirect:/librarian/users";
  }
}