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
  // Reports cần hiện cả số sách và lượt mượn; không set bookCount = 0L ở query này.
  @Query("SELECT new com.library.dto.CategoryDTO(c.id, c.name, COUNT(DISTINCT b), COUNT(br)) " +
       "FROM Category c " +
       "LEFT JOIN c.books b " +
       "LEFT JOIN BorrowRecord br ON br.book.id = b.id " +
       "GROUP BY c.id, c.name")
    List<CategoryDTO> findAllWithBorrowCount();
}
