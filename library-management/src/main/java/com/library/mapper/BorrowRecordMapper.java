package com.library.mapper;

import com.library.dto.BorrowRecordDTO;
import com.library.model.BorrowRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BorrowRecordMapper {

  // --- Hàm 1: Map đầy đủ (Dùng cho trang quản lý mượn trả chung) ---
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.fullName", target = "userName")
  @Mapping(source = "book.id", target = "bookId")
  @Mapping(source = "book.title", target = "bookTitle")
  BorrowRecordDTO toDTO(BorrowRecord borrowRecord);

  // --- Hàm 2: Bỏ qua User (Dùng cho lịch sử cá nhân trong UserDTO) ---
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "userName", ignore = true)
  @Mapping(source = "book.id", target = "bookId")
  @Mapping(source = "book.title", target = "bookTitle")
  BorrowRecordDTO toDTOWithoutUser(BorrowRecord borrowRecord);

  @Mapping(target = "user.id", source = "userId")
  @Mapping(target = "book.id", source = "bookId")
  BorrowRecord toEntity(BorrowRecordDTO borrowRecordDTO);
}