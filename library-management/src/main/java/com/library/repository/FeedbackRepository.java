package com.library.repository;

import com.library.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByUserId(Long userId);
    List<Feedback> findByBookId(Long bookId);

    // Tổng weight của từng sách theo user — dùng cho AI
    @Query("SELECT f.book.id, SUM(f.weight) FROM Feedback f " +
           "WHERE f.user.id = :userId GROUP BY f.book.id")
    List<Object[]> findWeightedBooksByUserId(@Param("userId") Long userId);
}