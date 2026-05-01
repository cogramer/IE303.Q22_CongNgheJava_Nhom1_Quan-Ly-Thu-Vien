package com.library.ai;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SimilarityUtil {

    // Tính Cosine Similarity giữa 2 vector
    // Vector ở đây là: key = bookId, value = số lần mượn
    public double cosineSimilarity(Map<Long, Double> vectorA, Map<Long, Double> vectorB) {
        if (vectorA.isEmpty() || vectorB.isEmpty()) return 0.0;

        // Tính dot product (tích vô hướng)
        double dotProduct = 0.0;
        for (Map.Entry<Long, Double> entry : vectorA.entrySet()) {
            if (vectorB.containsKey(entry.getKey())) {
                dotProduct += entry.getValue() * vectorB.get(entry.getKey());
            }
        }

        // Tính độ lớn của 2 vector
        double magnitudeA = Math.sqrt(vectorA.values().stream()
                .mapToDouble(v -> v * v).sum());
        double magnitudeB = Math.sqrt(vectorB.values().stream()
                .mapToDouble(v -> v * v).sum());

        if (magnitudeA == 0 || magnitudeB == 0) return 0.0;

        return dotProduct / (magnitudeA * magnitudeB);
    }
}