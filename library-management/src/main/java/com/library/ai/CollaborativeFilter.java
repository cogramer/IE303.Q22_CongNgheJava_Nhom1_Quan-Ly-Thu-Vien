package com.library.ai;

import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.FeedbackRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CollaborativeFilter {

    private final BorrowRecordRepository borrowRepository;
    private final UserRepository userRepository;
    private final SimilarityUtil similarityUtil;
    private final FeedbackRepository feedbackRepository;

    private static final int TOP_SIMILAR_USERS = 5;
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final double SIMILARITY_THRESHOLD = 0.1;

    public List<Long> recommend(Long userId) {
        Map<Long, Double> targetVector = buildUserVector(userId);

        if (targetVector.isEmpty()) {
            return getMostBorrowedBooks(userId);
        }

        List<User> allUsers = userRepository.findAll();
        Map<Long, Double> similarityScores = new HashMap<>();

        for (User other : allUsers) {
            if (other.getId().equals(userId)) continue;

            Map<Long, Double> otherVector = buildUserVector(other.getId());
            if (otherVector.isEmpty()) continue;

            double similarity = similarityUtil.cosineSimilarity(targetVector, otherVector);
            if (similarity >= SIMILARITY_THRESHOLD) {
                similarityScores.put(other.getId(), similarity);
            }
        }

        if (similarityScores.isEmpty()) {
            return getMostBorrowedBooks(userId);
        }

        List<Long> topSimilarUsers = similarityScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(TOP_SIMILAR_USERS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        Set<Long> alreadyBorrowed = targetVector.keySet();
        Map<Long, Double> bookScores = new HashMap<>();

        for (Long similarUserId : topSimilarUsers) {
            double weight = similarityScores.get(similarUserId);
            Map<Long, Double> similarVector = buildUserVector(similarUserId);

            for (Map.Entry<Long, Double> entry : similarVector.entrySet()) {
                Long bookId = entry.getKey();
                if (!alreadyBorrowed.contains(bookId)) {
                    bookScores.merge(bookId, weight * entry.getValue(), Double::sum);
                }
            }
        }

        List<Long> result = bookScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(MAX_RECOMMENDATIONS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (result.size() < MAX_RECOMMENDATIONS) {
            List<Long> popular = getMostBorrowedBooks(userId);
            for (Long bookId : popular) {
                if (!result.contains(bookId) && result.size() < MAX_RECOMMENDATIONS) {
                    result.add(bookId);
                }
            }
        }

        return result;
    }

    // Dùng feedback weight thay vì đếm số lần mượn
    private Map<Long, Double> buildUserVector(Long userId) {
        List<Object[]> results = feedbackRepository.findWeightedBooksByUserId(userId);

        // Nếu chưa có feedback thì fallback về borrow_records
        if (results.isEmpty()) {
            return buildVectorFromBorrowRecords(userId);
        }

        Map<Long, Double> vector = new HashMap<>();
        for (Object[] row : results) {
            Long bookId = (Long) row[0];
            Double totalWeight = ((Number) row[1]).doubleValue();
            vector.put(bookId, totalWeight);
        }
        return vector;
    }

    // Fallback: dùng borrow_records nếu chưa có feedback
    private Map<Long, Double> buildVectorFromBorrowRecords(Long userId) {
        List<BorrowRecord> records = borrowRepository.findByUserId(userId);
        Map<Long, Double> vector = new HashMap<>();
        for (BorrowRecord record : records) {
            Long bookId = record.getBook().getId();
            vector.merge(bookId, 1.0, Double::sum);
        }
        return vector;
    }

    private List<Long> getMostBorrowedBooks(Long userId) {
        Set<Long> alreadyBorrowed = buildUserVector(userId).keySet();

        return borrowRepository.findAll().stream()
            .map(r -> r.getBook().getId())
            .filter(bookId -> !alreadyBorrowed.contains(bookId))
            .collect(Collectors.groupingBy(
                bookId -> bookId,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(MAX_RECOMMENDATIONS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}