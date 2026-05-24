package com.library.mapper;

import org.springframework.stereotype.Component;

import com.library.dto.ReservationDTO;
import com.library.model.Reservation;

@Component
public class ReservationMapper {

  public ReservationDTO toDTO(Reservation entity) {
    if (entity == null) {
      return null;
    }

    ReservationDTO dto = new ReservationDTO();
    dto.setId(entity.getId());
    dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);

    if (entity.getUser() != null) {
      dto.setUserId(entity.getUser().getId());
      dto.setUsername(entity.getUser().getUsername());
      dto.setFullName(entity.getUser().getFullName());
    }

    if (entity.getBook() != null) {
      dto.setBookId(entity.getBook().getId());
      dto.setBookTitle(entity.getBook().getTitle());
    }

    return dto;
  }
}