package com.library.repository;

import com.library.dto.CategoryDTO;
import com.library.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
  // Giải quyết hàm 1: Đếm số lượng sách mỗi thể loại
  @Query("SELECT new com.library.dto.CategoryDTO(c.id, c.name, COUNT(b), 0L) " +
      "FROM Category c LEFT JOIN c.books b GROUP BY c.id, c.name")
  List<CategoryDTO> findAllWithBookCount();

  // Giải quyết hàm 2: Đếm lượt mượn mỗi thể loại (Thể loại HOT)
  // Query này đi từ Category -> Book -> BorrowRecords
  @Query("SELECT new com.library.dto.CategoryDTO(c.id, c.name, 0L, COUNT(br)) " +
       "FROM Category c " +
       "LEFT JOIN c.books b " +
       "LEFT JOIN BorrowRecord br ON br.book.id = b.id " + // <--- SỬA DÒNG NÀY
       "GROUP BY c.id, c.name")
    List<CategoryDTO> findAllWithBorrowCount();
}