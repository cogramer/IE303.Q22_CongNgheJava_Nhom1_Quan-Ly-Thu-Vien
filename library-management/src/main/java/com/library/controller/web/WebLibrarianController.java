package com.library.controller.web;

import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.library.dto.BookDTO;
import com.library.dto.UserDTO;
import com.library.service.BookService;
import com.library.service.BorrowRecordService;
import com.library.service.CategoryService;
import com.library.service.ImageStorageService;
import com.library.service.ReservationService;
import com.library.service.UserService;
import com.library.enums.RegisterResult;
import com.library.model.User;

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
  private final ImageStorageService imageStorageService;

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
  public String saveBook(@ModelAttribute BookDTO bookDTO,
      @RequestParam(name = "bookImage", required = false) MultipartFile bookImage,
      RedirectAttributes redirectAttributes) {
    try {
      String uploadedImageUrl = imageStorageService.storeBookImage(bookImage);
      if (uploadedImageUrl != null) {
        bookDTO.setImageUrl(uploadedImageUrl);
      }

      bookService.saveBook(bookDTO);
      // Nếu thành công, gửi thông báo màu xanh
      redirectAttributes.addFlashAttribute("successMsg", "Lưu sách thành công!");
    } catch (IllegalArgumentException e) {
      // Bắt lỗi trùng ISBN hoặc lỗi số lượng, gửi thông báo màu đỏ
      redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
    } catch (Exception e) {
      // Bắt các lỗi hệ thống khác
      redirectAttributes.addFlashAttribute("errorMsg", "Đã xảy ra lỗi không xác định: " + e.getMessage());
    }

    return "redirect:/librarian/books";
  }

  // Xử lý Xóa sách
  @PostMapping("/books/delete/{id}")
  public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      bookService.deleteBook(id);
      redirectAttributes.addFlashAttribute("successMsg", "Xóa sách thành công!");
    } catch (DataIntegrityViolationException e) {
      redirectAttributes.addFlashAttribute("errorMsg",
          "Không thể xóa sách này vì sách đang có phiếu mượn, đặt giữ hoặc đánh giá liên quan.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMsg", "Không thể xóa sách: " + e.getMessage());
    }

    return "redirect:/librarian/books";
  }

  // Xử lý Duyệt (Fulfill) đặt trước sách
  // Trong WebLibrarianController.java

  @PostMapping("/reservations/fulfill/{id}")
  public String fulfillReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      reservationService.fulfillReservation(id);
      redirectAttributes.addFlashAttribute("successMsg", "Duyệt yêu cầu đặt giữ thành công!");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMsg", "Không thể duyệt: " + e.getMessage());
    }
    return "redirect:/librarian/reservations";
  }

  // Xử lý xác nhận trả sách (1 hoặc nhiều cuốn)
  @PostMapping("/loans/return")
  public String returnLoans(@RequestParam("loanIds") List<Long> loanIds) {
    // Gọi service xử lý trả sách theo list ID
    borrowService.returnMultipleBooks(loanIds);
    return "redirect:/librarian/loans";
  }

  @PostMapping("/loans/renew/{id}")
  public String renewLoanWeb(@PathVariable Long id, @RequestParam(name = "days", defaultValue = "7") int days,
      RedirectAttributes redirectAttributes) {
    try {
      borrowService.renewLoan(id, days);
      redirectAttributes.addFlashAttribute("successMsg", "Gia hạn phiếu mượn thành công!");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMsg", "Không thể gia hạn: " + e.getMessage());
    }
    return "redirect:/librarian/loans";
  }

  @PostMapping("/users/save")
  public String saveUser(@ModelAttribute UserDTO userDTO, RedirectAttributes redirectAttributes) {
    try {
      if (userDTO.getId() != null) {
        // Cập nhật thông tin cơ bản
        userService.updateUser(userDTO.getId(), userDTO);

        // Cập nhật vai trò (Role)
        if (userDTO.getRole() != null) {
          userService.updateUserRole(userDTO.getId(), userDTO.getRole());
        }
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật người dùng thành công!");
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMsg", "Lỗi cập nhật: " + e.getMessage());
    }
    return "redirect:/librarian/users";
  }

  // THÊM MỚI TÍNH NĂNG: Thêm người dùng
  @PostMapping("/users/add")
  public String addUser(@RequestParam String username,
      @RequestParam String password,
      @RequestParam String fullName,
      @RequestParam String email,
      @RequestParam User.Role role,
      RedirectAttributes redirectAttributes) {
    try {
      RegisterResult result;
      // Tuỳ theo role mà gọi hàm Service tương ứng
      if (role == User.Role.LIBRARIAN) {
        result = userService.createStaff(username, password, email, fullName, role);
      } else {
        result = userService.addNewUser(username, password, email, fullName);
      }

      if (result == RegisterResult.SUCCESS) {
        redirectAttributes.addFlashAttribute("successMsg", "Thêm người dùng thành công!");
      } else {
        redirectAttributes.addFlashAttribute("errorMsg", "Thất bại: " + result.name());
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMsg", "Lỗi hệ thống: " + e.getMessage());
    }
    return "redirect:/librarian/users";
  }

  // Xử lý Xóa người dùng (Bổ sung thông báo)
  @PostMapping("/users/delete")
  public String deleteUser(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
    try {
      userService.deleteUser(id);
      redirectAttributes.addFlashAttribute("successMsg", "Đã xóa người dùng!");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMsg", buildUserDeleteErrorMessage(e));
    }
    return "redirect:/librarian/users";
  }

  @PostMapping("/users/delete/{id}")
    public String deleteUserByPath(@PathVariable Long id, RedirectAttributes redirectAttributes) {
      try {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa người dùng!");
      } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMsg", buildUserDeleteErrorMessage(e));
      }
        return "redirect:/librarian/users";
    }

  // UI librarian/users dùng modal sửa user và submit về endpoint này.
  @PostMapping("/users/update/{id}")
  public String updateUser(@PathVariable Long id, @ModelAttribute UserDTO userDTO) {
      userService.updateUser(id, userDTO);
      return "redirect:/librarian/users";
  }

  private String buildUserDeleteErrorMessage(Exception e) {
    if (e instanceof DataIntegrityViolationException) {
      return "Không thể xóa người dùng này vì đang có phiếu mượn, đặt giữ, đánh giá hoặc dữ liệu liên quan.";
    }
    return "Không thể xóa người dùng: " + e.getMessage();
  }
}
