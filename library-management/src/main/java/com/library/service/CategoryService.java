package com.library.service;

import com.library.dto.CategoryDTO;
import com.library.mapper.CategoryMapper;
import com.library.model.Category;
import com.library.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private CategoryMapper categoryMapper;

  // --- 1. CRUD: THÊM, SỬA, XÓA ---

  public List<CategoryDTO> getAllCategories() {
    return categoryRepository.findAll().stream()
        .map(categoryMapper::toDTO)
        .collect(Collectors.toList());
  }

  public CategoryDTO saveCategory(CategoryDTO dto) {
    Category entity = categoryMapper.toEntity(dto);
    Category saved = categoryRepository.save(entity);
    return categoryMapper.toDTO(saved);
  }

  public void deleteCategory(Long id) {
    categoryRepository.deleteById(id);
  }

  // --- 2. THỐNG KÊ SỐ LƯỢNG SÁCH MỖI THỂ LOẠI ---
  public List<CategoryDTO> getBookCountByCategory() {
    return categoryRepository.findAllWithBookCount();
  }

  // --- 3. THỐNG KÊ THỂ LOẠI HOT (DỰA TRÊN LƯỢT MƯỢN) ---
  public List<CategoryDTO> getCategoryHotStats() {
    return categoryRepository.findAllWithBorrowCount();
  }
}