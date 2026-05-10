package com.library.repository;

import com.library.model.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByUserId(Long userId);

    List<BorrowRecord> findByBookId(Long bookId);

    List<BorrowRecord> findByStatus(BorrowRecord.Status status);

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, BorrowRecord.Status status);

    // 10 phiếu mượn gần nhất
    List<BorrowRecord> findTop10ByOrderByBorrowDateDesc();

    // Top sách mượn nhiều nhất
    @Query("SELECT b.book, COUNT(b) FROM BorrowRecord b GROUP BY b.book ORDER BY COUNT(b) DESC")
    List<Object[]> findTopBorrowedBooks(Pageable pageable);

    // Thống kê theo tháng
    @Query("SELECT MONTH(b.borrowDate), COUNT(b) FROM BorrowRecord b " +
        "WHERE YEAR(b.borrowDate) = :year GROUP BY MONTH(b.borrowDate) ORDER BY MONTH(b.borrowDate)")
    List<Object[]> countByMonth(@Param("year") int year);
}

