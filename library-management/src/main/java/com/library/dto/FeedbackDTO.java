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
        private Integer score;
        private String comment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private Integer score;
        private String comment;
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
        private Integer score;
        private String comment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private Long bookId;
        private long totalFeedback;
    }
}
