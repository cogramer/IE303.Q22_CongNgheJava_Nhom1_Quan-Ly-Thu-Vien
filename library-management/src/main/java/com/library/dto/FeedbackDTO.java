package com.library.dto;

import java.time.LocalDateTime;

import com.library.model.Feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FeedbackDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private Long bookId;
        private Feedback.EventType eventType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private Feedback.EventType eventType;
        private Float weight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long userId;
        private String username;
        private Long bookId;
        private String bookTitle;
        private Feedback.EventType eventType;
        private Float weight;
        private LocalDateTime eventDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private Long bookId;
        private long totalFeedback;
    }
}
