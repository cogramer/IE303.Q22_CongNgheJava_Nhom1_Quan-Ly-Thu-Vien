package com.library.service;

import com.library.ai.CollaborativeFilter;
import com.library.dto.BookDTO;
import com.library.mapper.BookMapper;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final CollaborativeFilter collaborativeFilter;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public List<BookDTO> recommendBooks(Long userId) {
        List<Long> bookIds = collaborativeFilter.recommend(userId);

        return bookIds.stream()
                .map(bookRepository::findById)
                .filter(Optional::isPresent)
                .map(optional -> bookMapper.toDTO(optional.get()))
                .collect(Collectors.toList());
    }
}