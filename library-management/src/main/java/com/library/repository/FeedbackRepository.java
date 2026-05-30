package com.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.library.model.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByUserId(Long userId, Pageable pageable);
    Page<Feedback> findByBookId(Long bookId, Pageable pageable);
    Page<Feedback> findByUserIdAndEventType(Long userId, Feedback.EventType eventType, Pageable pageable);
    Page<Feedback> findByBookIdAndEventType(Long bookId, Feedback.EventType eventType, Pageable pageable);

    // Tổng weight của từng sách theo user
    @Query("SELECT f.book.id, SUM(f.weight) FROM Feedback f " +
           "WHERE f.user.id = :userId GROUP BY f.book.id")
    List<Object[]> findWeightedBooksByUserId(@Param("userId") Long userId);

    // Tính điểm trung bình của sách dựa trên các đánh giá RATING
    @Query("SELECT AVG(f.score) FROM Feedback f WHERE f.book.id = :bookId AND f.eventType = 'RATING' AND f.score IS NOT NULL")
    Optional<Double> findAverageScoreByBookId(@Param("bookId") Long bookId);
}
