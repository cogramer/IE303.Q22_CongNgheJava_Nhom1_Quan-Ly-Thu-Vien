package com.library.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

  private static final Path BOOK_UPLOAD_DIR = Paths.get("src", "main", "resources", "static", "img", "books");
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

  public String storeBookImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    String extension = getExtension(file.getOriginalFilename());
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new IllegalArgumentException("Anh bia chi ho tro JPG, JPEG, PNG, WEBP hoac GIF.");
    }

    try {
      Files.createDirectories(BOOK_UPLOAD_DIR);
      String fileName = UUID.randomUUID() + "." + extension;
      Path target = BOOK_UPLOAD_DIR.resolve(fileName).normalize();

      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
      }

      return "/img/books/" + fileName;
    } catch (IOException e) {
      throw new IllegalArgumentException("Khong the luu anh bia: " + e.getMessage(), e);
    }
  }

  private String getExtension(String originalFilename) {
    String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
    int dotIndex = filename.lastIndexOf('.');

    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      throw new IllegalArgumentException("File anh can co phan mo rong hop le.");
    }

    return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
  }
}
