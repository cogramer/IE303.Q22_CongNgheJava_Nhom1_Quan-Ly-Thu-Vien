package com.library.service;

import com.library.dto.BorrowRecordDTO;
import com.library.dto.UserDTO;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  // Cần thêm Mapper của BorrowRecord để convert lịch sử
  private final BorrowRecordMapper borrowMapper;
  private final BorrowRecordRepository borrowRepository;

  // --- 1. LẤY DANH SÁCH ---
  public List<UserDTO> getAllUsers() {
    return userRepository.findAll().stream()
        .map(userMapper::toDTO)
        .collect(Collectors.toList());
  }

  // --- 2. TÌM KIẾM QUA EMAIL ---
  public UserDTO getUserByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với email: " + email));
    return userMapper.toDTO(user);
  }

  // --- 3. THÊM NGƯỜI DÙNG (ĐĂNG KÝ) ---
  @Transactional
  public UserDTO createUser(UserDTO dto) {
    if (userRepository.existsByUsername(dto.getUsername())) {
      throw new RuntimeException("Tên đăng nhập đã tồn tại!");
    }
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new RuntimeException("Email đã được sử dụng!");
    }

    User user = userMapper.toEntity(dto);

    // CHÚ Ý: Sau này nên dùng passwordEncoder.encode(dto.getPassword()) tại đây
    user.setPassword(dto.getPassword());

    if (user.getRole() == null) {
      user.setRole(User.Role.READER); // Mặc định là người đọc
    }

    return userMapper.toDTO(userRepository.save(user));
  }

  // --- 4. CẬP NHẬT THÔNG TIN ---
  @Transactional
  public UserDTO updateUser(Long id, UserDTO dto) {
    User existingUser = userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng ID: " + id));

    existingUser.setFullName(dto.getFullName());
    existingUser.setRole(dto.getRole());

    // Chỉ cập nhật email nếu không bị trùng với người khác
    if (!existingUser.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
      throw new RuntimeException("Email mới đã được người khác sử dụng!");
    }
    existingUser.setEmail(dto.getEmail());

    return userMapper.toDTO(userRepository.save(existingUser));
  }

  // --- 5. XÓA NGƯỜI DÙNG ---
  public void deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
      throw new EntityNotFoundException("Không tìm thấy người dùng để xóa!");
    }
    userRepository.deleteById(id);
  }

  // --- 6. TÌM KIẾM THEO TÊN (FULLNAME) ---

  public List<UserDTO> searchUsersByName(String name) {
    return userRepository.findByFullNameContainingIgnoreCase(name).stream()
        .map(userMapper::toDTO)
        .collect(Collectors.toList());
  }

  // --- 7. TÌM KIẾM THEO USERNAME ---
  public UserDTO getUserByUsername(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user: " + username));
    return userMapper.toDTO(user);
  }

  // --- 8. KIỂM TRA ĐĂNG NHẬP (BASIC) ---
  public UserDTO login(String username, String password) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("Sai tên đăng nhập hoặc mật khẩu!"));

    // GIẢ SỬ CHƯA MÃ HÓA (SO SÁNH TRỰC TIẾP)
    if (!user.getPassword().equals(password)) {
      throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu!");
    }

    /*
     * NẾU DÙNG MÃ HÓA BCrypt (Sau này bạn nâng cấp):
     * if (!passwordEncoder.matches(password, user.getPassword())) {
     * throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu!");
     * }
     */

    return userMapper.toDTO(user);
  }

  // --- THỐNG KÊ LỊCH SỬ MƯỢN CỦA USER ---
  public List<BorrowRecordDTO> getUserBorrowHistory(Long userId) {
    // Kiểm tra user có tồn tại không
    if (!userRepository.existsById(userId)) {
      throw new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId);
    }

    // Lấy tất cả bản ghi mượn của User này
    return borrowRepository.findByUserId(userId).stream()
        .map(borrowMapper::toDTOWithoutUser)
        .collect(Collectors.toList());
  }
}