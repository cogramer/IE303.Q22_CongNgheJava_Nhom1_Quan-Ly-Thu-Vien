package com.library.controller.api;

import com.library.dto.BorrowRecordDTO;
import com.library.dto.UserDTO;
import com.library.enums.RegisterResult;
import com.library.enums.Result;
import com.library.model.User;
import com.library.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

  private final UserService userService;

  // --- 1. LẤY DANH SÁCH & TÌM KIẾM THEO TÊN ---
  // Phân quyền: Chỉ Thủ thư và Admin mới được xem danh sách hoặc tìm kiếm users
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping
  public ResponseEntity<List<UserDTO>> getUsers(@RequestParam(required = false) String name) {
    if (name != null && !name.trim().isEmpty()) {
      return ResponseEntity.ok(userService.searchUsersByName(name));
    }
    return ResponseEntity.ok(userService.getAllUsers());
  }

  // --- 2. LẤY CHI TIẾT 1 USER THEO USERNAME ---
  // Phân quyền: Admin, Thủ thư có thể xem bất kỳ ai. Người dùng chỉ xem được
  // chính mình.
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or authentication.name == #username")
  @GetMapping("/{username}")
  public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
    return ResponseEntity.ok(userService.getUserByUsername(username));
  }

  // --- 3. THÊM NGƯỜI DÙNG MỚI (ĐỘC GIẢ) ---
  // Phân quyền: Đăng ký độc giả thường do Admin/Thủ thư cấp (hoặc nếu là API đăng
  // ký public thì để permitAll ở SecurityConfig)
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PostMapping
  public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO) {
    RegisterResult result = userService.addNewUser(
        userDTO.getUsername(),
        userDTO.getPassword(),
        userDTO.getEmail(),
        userDTO.getFullName());

    if (result == RegisterResult.SUCCESS) {
      return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Tạo người dùng thành công"));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.name()));
  }

  // --- 4. CẬP NHẬT THÔNG TIN ---
  // Phân quyền: Chỉ cập nhật nếu là Admin thao tác, HOẶC user đang cập nhật chính
  // ID của mình
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
  @PutMapping("/{id}")
  public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
    UserDTO updatedUser = userService.updateUser(id, userDTO);
    return ResponseEntity.ok(updatedUser);
  }

  // --- 5. XÓA NGƯỜI DÙNG ---
  // Phân quyền: Chỉ duy nhất ADMIN mới có quyền xóa tài khoản
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  // --- 6. XEM LỊCH SỬ MƯỢN SÁCH CỦA USER ---
  // Phân quyền: Thủ thư/Admin tra cứu, hoặc Độc giả tự xem lịch sử của mình
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or #id == authentication.principal.id")
  @GetMapping("/{id}/borrow-history")
  public ResponseEntity<List<BorrowRecordDTO>> getUserBorrowHistory(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserBorrowHistory(id));
  }

  // --- 7. ĐỔI MẬT KHẨU ---
  // Phân quyền: Phải đăng nhập và chỉ được đổi mật khẩu của chính username đang
  // đăng nhập
  @PreAuthorize("authentication.name == #payload['username']")
  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload) {
    String username = payload.get("username");
    String oldPassword = payload.get("oldPassword");
    String newPassword = payload.get("newPassword");

    Result result = userService.changePassword(username, oldPassword, newPassword);

    if (result == Result.SUCCESS) {
      return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.name()));
  }

  // --- 8. TẠO TÀI KHOẢN NHÂN VIÊN (THỦ THƯ) ---
  // Phân quyền: CHỈ ADMIN
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/staff")
  public ResponseEntity<?> createStaff(@Valid @RequestBody UserDTO userDTO) {
    // Nếu request không gửi role, mặc định báo lỗi hoặc ép thành LIBRARIAN
    User.Role roleToCreate = userDTO.getRole() != null ? userDTO.getRole() : User.Role.LIBRARIAN;

    try {
      RegisterResult result = userService.createStaff(
          userDTO.getUsername(),
          userDTO.getPassword(),
          userDTO.getEmail(),
          userDTO.getFullName(),
          roleToCreate);

      if (result == RegisterResult.SUCCESS) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Tạo tài khoản Thủ thư thành công"));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.name()));

    } catch (IllegalArgumentException e) {
      // Bắt lỗi cố tình tạo Admin hoặc Reader qua API này
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
  }

  // --- 9. CẤP QUYỀN / ĐỔI QUYỀN CHO USER CÓ SẴN ---
  // Phân quyền: CHỈ ADMIN
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{id}/role")
  public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestParam User.Role newRole) {
    try {
      UserDTO updatedUser = userService.updateUserRole(id, newRole);
      return ResponseEntity.ok(updatedUser);
    } catch (IllegalArgumentException e) {
      // Bắt lỗi cố tình cấp quyền Admin
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
  }
}