package com.library.mapper;

import com.library.dto.UserDTO;
import com.library.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.library.mapper.BorrowRecordMapper;

@Mapper(componentModel = "spring", uses = { BorrowRecordMapper.class })
public interface UserMapper {

  @Mapping(target = "password", ignore = true) // Tuyệt đối không map password ra DTO khi trả về client
  @Mapping(target = "totalBorrowCount", expression = "java(user.getBorrowRecords() != null ? user.getBorrowRecords().size() : 0)")
  UserDTO toDTO(User user);

  @Mapping(target = "borrowRecords", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  User toEntity(UserDTO dto);
}