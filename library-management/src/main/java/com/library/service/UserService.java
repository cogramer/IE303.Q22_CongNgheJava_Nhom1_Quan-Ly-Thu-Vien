package com.library.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dto.BorrowRecordDTO;
import com.library.dto.UserDTO;
import com.library.enums.RegisterResult;
import com.library.enums.Result;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.UserMapper;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
  @Autowired
  private PasswordEncoder passwordEncoder;

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

  public Result checkEmail(String username, String email) {
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
      return Result.EMAIL_NOT_FOUND;
    }

    User user = userOpt.get();
    
    if (!username.equals(user.getUsername())) {
      return Result.USERNAME_NOT_MATCH;
    }
    return Result.SUCCESS;
  }

  // --- 3. THÊM NGƯỜI DÙNG (ĐĂNG KÝ) ---
  public RegisterResult addNewUser(String username, String password, String email, String fullName) {
    if (userRepository.existsByUsername(username)) {
        return RegisterResult.USERNAME_EXIST;
    }

    if (userRepository.existsByEmail(email)) {
        return RegisterResult.EMAIL_EXIST;
    }  
    
    User newUser = new User();
    newUser.setUsername(username);
    newUser.setPassword(passwordEncoder.encode(password));
    newUser.setEmail(email);
    newUser.setFullName(fullName);
    newUser.setRole(User.Role.READER); // mặc định role là READER
    newUser.setCreatedAt(java.time.LocalDateTime.now());
    userRepository.save(newUser);
    return RegisterResult.SUCCESS;
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

  // --- 6. TÌM KIẾM THEO TÊN ---
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

  // Đặt lại mật khẩu
  public Result resetPassword(String email, String newPassword) {
    Optional<User> userOpt = userRepository.findByEmail(email);

    if (userOpt.isEmpty()) {
      return Result.EMAIL_NOT_FOUND;
    }

    User user = userOpt.get();

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    return Result.SUCCESS;
  }

  //Đổi mật khẩu
  public Result changePassword(String username, String oldPassword, String newPassword) {
    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
        return Result.USERNAME_NOT_FOUND;
    }

    User user = userOpt.get();

    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
        return Result.PASSWORD_INCORRECT;
    } else if (passwordEncoder.matches(newPassword, user.getPassword())) {
        return Result.NEW_PASSWORD_SAME_AS_OLD_PASSWORD;
    }
    
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    return Result.SUCCESS;
  }

}