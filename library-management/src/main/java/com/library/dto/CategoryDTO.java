package com.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL) // Các field null sẽ không xuất hiện trong JSON
public class CategoryDTO {

  private Long id;

  @NotBlank(message = "Name không được để trống")
  @Size(max = 100, message = "Name tối đa 100 ký tự")
  private String name;

  @Size(max = 255, message = "Description tối đa 255 ký tự")
  private String description;

  // --- Các thuộc tính thống kê, không bắt buộc ---
  private Long bookCount; // số lượng sách
  private Long borrowCount; // số lượt mượn

  public CategoryDTO() {
  }

  public CategoryDTO(Long id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public CategoryDTO(Long id, String name, String description, Long bookCount, Long borrowCount) {
    this(id, name, description);
    this.bookCount = bookCount;
    this.borrowCount = borrowCount;
  }

  // Getter & Setter
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getBookCount() {
    return bookCount;
  }

  public void setBookCount(Long bookCount) {
    this.bookCount = bookCount;
  }

  public Long getBorrowCount() {
    return borrowCount;
  }

  public void setBorrowCount(Long borrowCount) {
    this.borrowCount = borrowCount;
  }
}