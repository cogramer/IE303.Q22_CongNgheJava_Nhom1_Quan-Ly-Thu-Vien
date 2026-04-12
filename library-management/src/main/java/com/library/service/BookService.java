package com.library.service;

import java.util.stream.*;
import com.library.dto.BookDTO;
import com.library.mapper.BookMapper;
import com.library.model.Book;
import com.library.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

  private final BookRepository bookRepository;
  private final BookMapper bookMapper;

  // --- 1. LẤY DANH SÁCH ---
  public List<BookDTO> getAllBooks() {
    return bookRepository.findAll().stream()
        .map(bookMapper::toDTO)
        .collect(Collectors.toList());
  }

  // --- 2. LẤY SÁCH THEO ID ---
  public BookDTO getBookById(Long id) {
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách ID: " + id));
    return bookMapper.toDTO(book);
  }

  // --- 3. THÊM & SỬA ---
  @Transactional
  public BookDTO saveBook(BookDTO dto) {
    Book entity = bookMapper.toEntity(dto);
    // Khi tạo mới, ta mặc định sẵn có = tổng số lượng nếu không được set
    if (dto.getId() == null) {
      entity.setAvailableCopies(dto.getTotalCopies());
    }
    Book saved = bookRepository.save(entity);
    return bookMapper.toDTO(saved);
  }

  // --- 4. XÓA ---
  public void deleteBook(Long id) {
    if (!bookRepository.existsById(id)) {
      throw new EntityNotFoundException("Không thể xóa, không tìm thấy sách ID: " + id);
    }
    bookRepository.deleteById(id);
  }

  public List<BookDTO> searchBooks(String keyword) {
    List<Book> byTitle = bookRepository.findByTitleContainingIgnoreCase(keyword);
    List<Book> byAuthor = bookRepository.findByAuthorContainingIgnoreCase(keyword);

    // Gộp 2 danh sách, loại trùng lặp
    return Stream.concat(byTitle.stream(), byAuthor.stream())
        .distinct()
        .map(bookMapper::toDTO)
        .collect(Collectors.toList());
}

  // --- 5. THAY ĐỔI TỔNG SỐ LƯỢNG (KHI NHẬP THÊM SÁCH MỚI) ---
  @Transactional
  public BookDTO updateTotalCopies(Long id, int newTotal) {
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Sách không tồn tại"));

    int diff = newTotal - book.getTotalCopies();
    book.setTotalCopies(newTotal);
    // Khi tăng tổng số lượng, số lượng sẵn có cũng tăng tương ứng
    book.setAvailableCopies(book.getAvailableCopies() + diff);

    return bookMapper.toDTO(bookRepository.save(book));
  }

  // --- 6. THAY ĐỔI SỐ LƯỢNG TỒN KHO (KHI CHO MƯỢN/TRẢ SÁCH) ---
  @Transactional
  public void updateAvailableCopies(Long id, int amount) {
    Book book = bookRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Sách không tồn tại"));

    int newAvailable = book.getAvailableCopies() + amount;

    if (newAvailable < 0) {
      throw new RuntimeException("Số lượng sách trong kho không đủ để thực hiện giao dịch này!");
    }
    if (newAvailable > book.getTotalCopies()) {
      throw new RuntimeException("Số lượng sẵn có không thể lớn hơn tổng số lượng sách!");
    }

    book.setAvailableCopies(newAvailable);
    bookRepository.save(book);
  }
}