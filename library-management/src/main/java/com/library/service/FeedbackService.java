package com.library.service;

import com.library.model.Book;
import com.library.model.Feedback;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.FeedbackRepository;
import com.library.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        float weight = switch (eventType) {
            case BORROW -> 1.0f;
            case RETURN -> 1.5f;
            case RATING -> 2.0f;
        };

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setBook(book);
        feedback.setEventType(eventType);
        feedback.setWeight(weight);

        feedbackRepository.save(feedback);
    }
}