package com.library;

import com.library.service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibraryManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementApplication.class, args);
	}

	@Bean
	public CommandLineRunner runAtStartup(BorrowRecordService borrowRecordService) {
		return args -> {
			// Apply an immediate overdue check at startup so UI reflects current status
			try {
				borrowRecordService.updateOverdueStatus();
			} catch (Exception e) {
				System.err.println("Failed to run overdue update at startup: " + e.getMessage());
			}
		};
	}

}
