package com.library.mapper;

import com.library.dto.BookDTO;
import com.library.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookMapper {

  // Map từ Category (Object) sang ID và Name (String/Long)
  @Mapping(source = "category.id", target = "categoryId")
  @Mapping(source = "category.name", target = "categoryName")
  BookDTO toDTO(Book book);

  // Ngược lại, khi lưu sách, MapStruct sẽ hiểu cần tìm Category theo ID
  @Mapping(source = "categoryId", target = "category.id")
  Book toEntity(BookDTO dto);
}