package com.library.ai;

import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
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

    // Số lượng user tương tự tối đa để xét
    private static final int TOP_SIMILAR_USERS = 5;
    // Số lượng sách gợi ý tối đa
    private static final int MAX_RECOMMENDATIONS = 10;
    // Ngưỡng similarity tối thiểu (0.0 - 1.0)
    private static final double SIMILARITY_THRESHOLD = 0.1;

    // Hàm chính: gợi ý sách cho user
    public List<Long> recommend(Long userId) {

        // 1. Lấy vector sách của user hiện tại
        Map<Long, Integer> targetVector = buildUserVector(userId);

        // Nếu user chưa mượn sách nào thì gợi ý sách mượn nhiều nhất
        if (targetVector.isEmpty()) {
            return getMostBorrowedBooks(userId);
        }

        // 2. Lấy tất cả user khác
        List<User> allUsers = userRepository.findAll();

        // 3. Tính similarity với từng user khác
        Map<Long, Double> similarityScores = new HashMap<>();
        for (User other : allUsers) {
            if (other.getId().equals(userId)) continue;

            Map<Long, Integer> otherVector = buildUserVector(other.getId());
            if (otherVector.isEmpty()) continue;

            double similarity = similarityUtil.cosineSimilarity(targetVector, otherVector);
            if (similarity >= SIMILARITY_THRESHOLD) {
                similarityScores.put(other.getId(), similarity);
            }
        }

        // Nếu không tìm thấy user tương tự
        if (similarityScores.isEmpty()) {
            return getMostBorrowedBooks(userId);
        }

        // 4. Lấy TOP user tương tự nhất
        List<Long> topSimilarUsers = similarityScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_SIMILAR_USERS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. Gom sách từ các user tương tự, loại bỏ sách user hiện tại đã mượn
        Set<Long> alreadyBorrowed = targetVector.keySet();
        Map<Long, Double> bookScores = new HashMap<>();

        for (Long similarUserId : topSimilarUsers) {
            double weight = similarityScores.get(similarUserId);
            Map<Long, Integer> similarVector = buildUserVector(similarUserId);

            for (Map.Entry<Long, Integer> entry : similarVector.entrySet()) {
                Long bookId = entry.getKey();
                if (!alreadyBorrowed.contains(bookId)) {
                    // Cộng điểm sách theo trọng số similarity
                    bookScores.merge(bookId, weight * entry.getValue(), Double::sum);
                }
            }
        }

        // 6. Sắp xếp sách theo điểm cao nhất và trả về
        List<Long> result = bookScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(MAX_RECOMMENDATIONS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Nếu không đủ gợi ý thì bổ sung sách mượn nhiều nhất
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

    // Xây dựng vector sách của 1 user: key = bookId, value = số lần mượn
    private Map<Long, Integer> buildUserVector(Long userId) {
        List<BorrowRecord> records = borrowRepository.findByUserId(userId);
        Map<Long, Integer> vector = new HashMap<>();
        for (BorrowRecord record : records) {
            Long bookId = record.getBook().getId();
            vector.merge(bookId, 1, Integer::sum);
        }
        return vector;
    }

    // Fallback: trả về sách được mượn nhiều nhất mà user chưa mượn
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