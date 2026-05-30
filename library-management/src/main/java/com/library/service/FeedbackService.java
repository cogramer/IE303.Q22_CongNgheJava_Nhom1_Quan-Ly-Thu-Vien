package com.library.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.dto.FeedbackDTO;
import com.library.model.Book;
import com.library.model.Feedback;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.FeedbackRepository;
import com.library.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional
    public void recordEvent(Long userId, Long bookId, Feedback.EventType eventType) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách"));

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setBook(book);
        feedback.setEventType(eventType);
        feedback.setWeight(getWeightForEventType(eventType));

        feedbackRepository.save(feedback);
    }

    @Transactional
    public FeedbackDTO.Response createFeedback(Long userId, FeedbackDTO.CreateRequest request) {
        validateCreateRequest(request);
        validateScoreAndComment(request.getScore(), request.getComment());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách"));

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setBook(book);
        feedback.setEventType(Feedback.EventType.RATING);
        feedback.setWeight(getWeightForEventType(Feedback.EventType.RATING));
        feedback.setScore(request.getScore());
        feedback.setComment(request.getComment());

        return toResponse(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public List<FeedbackDTO.Response> getFeedbackByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId);
        }

        return feedbackRepository.findByUserIdOrderByEventDateDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackDTO.Response> getFeedbackByBookId(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new EntityNotFoundException("Không tìm thấy sách ID: " + bookId);
        }

        return feedbackRepository.findByBookIdOrderByEventDateDesc(bookId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public FeedbackDTO.Response updateFeedback(Long feedbackId, Long actorUserId, boolean isStaff,
                                                     FeedbackDTO.UpdateRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy feedback ID: " + feedbackId));

        validateEditableFeedback(feedback);
        validateOwnership(feedback, actorUserId, isStaff);
        validateScoreAndComment(request.getScore(), request.getComment());

        feedback.setScore(request.getScore());
        feedback.setComment(request.getComment());

        return toResponse(feedbackRepository.save(feedback));
    }

    @Transactional
    public void deleteRatingFeedback(Long feedbackId, Long actorUserId, boolean isStaff) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy feedback ID: " + feedbackId));

        validateEditableFeedback(feedback);
        validateOwnership(feedback, actorUserId, isStaff);
        feedbackRepository.delete(feedback);
    }

    private void validateCreateRequest(FeedbackDTO.CreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payload feedback không hợp lệ");
        }
        if (request.getBookId() == null) {
            throw new IllegalArgumentException("bookId là bắt buộc");
        }
    }

    private void validateScoreAndComment(Integer score, String comment) {
        if (score == null) {
            throw new IllegalArgumentException("Điểm đánh giá là bắt buộc");
        }
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
        }
        if (comment != null && comment.length() > 1000) {
            throw new IllegalArgumentException("Bình luận không được vượt quá 1000 ký tự");
        }
    }

    private void validateEditableFeedback(Feedback feedback) {
        if (feedback.getEventType() != Feedback.EventType.RATING) {
            throw new IllegalArgumentException("Chỉ được sửa hoặc xóa feedback loại RATING");
        }
    }

    private void validateOwnership(Feedback feedback, Long actorUserId, boolean isStaff) {
        if (!isStaff && !feedback.getUser().getId().equals(actorUserId)) {
            throw new SecurityException("Không có quyền thao tác feedback này");
        }
    }

    private float getWeightForEventType(Feedback.EventType eventType) {
        return switch (eventType) {
            case BORROW -> 1.0f;
            case RETURN -> 1.5f;
            case RATING -> 2.0f;
        };
    }

    private FeedbackDTO.Response toResponse(Feedback feedback) {
        return new FeedbackDTO.Response(
            feedback.getId(),
            feedback.getUser().getId(),
            feedback.getUser().getUsername(),
            feedback.getBook().getId(),
            feedback.getBook().getTitle(),
            feedback.getEventType(),
            feedback.getWeight(),
            feedback.getEventDate(),
            feedback.getScore(),
            feedback.getComment()
        );
    }
}
