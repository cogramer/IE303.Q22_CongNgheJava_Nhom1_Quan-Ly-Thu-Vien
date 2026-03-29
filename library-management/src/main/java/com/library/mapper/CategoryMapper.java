package com.library.mapper;

import org.mapstruct.Mapping;
import com.library.dto.CategoryDTO;
import com.library.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
  @Mapping(target = "books", ignore = true)
  Category toEntity(CategoryDTO dto);

  @Mapping(target = "bookCount", ignore = true)
  @Mapping(target = "borrowCount", ignore = true)
  CategoryDTO toDTO(Category entity);
}