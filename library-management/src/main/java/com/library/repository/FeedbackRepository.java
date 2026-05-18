package com.library.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.library.model.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByUserId(Long userId);
    List<Feedback> findByBookId(Long bookId);
    List<Feedback> findByUserIdOrderByEventDateDesc(Long userId);
    List<Feedback> findByBookIdOrderByEventDateDesc(Long bookId);

    // Tổng weight của từng sách theo user
    @Query("SELECT f.book.id, SUM(f.weight) FROM Feedback f " +
           "WHERE f.user.id = :userId GROUP BY f.book.id")
    List<Object[]> findWeightedBooksByUserId(@Param("userId") Long userId);
}
