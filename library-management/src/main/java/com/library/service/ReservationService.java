package com.library.service;

import com.library.dto.ReservationDTO;
import com.library.mapper.ReservationMapper;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.Feedback; // Nếu bạn có class này
import com.library.model.Reservation;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.ReservationRepository;
import com.library.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final FeedbackService feedbackService; // Đảm bảo bạn đã Inject FeedbackService

    // Gọi Mapper mới tạo
    private final ReservationMapper reservationMapper;

    // Độc giả đặt giữ sách
    @Transactional
    public ReservationDTO createReservation(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách"));

        if (reservationRepository.existsByUserIdAndBookIdAndStatus(
                userId, bookId, Reservation.Status.PENDING)) {
            throw new RuntimeException("Bạn đã đặt giữ cuốn sách này rồi!");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(Reservation.Status.PENDING);

        return reservationMapper.toDTO(reservationRepository.save(reservation));
    }

    // Thủ thư xác nhận cho mượn → đổi reservation sang FULFILLED
    @Transactional
    public ReservationDTO fulfillReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đặt giữ"));

        Book book = reservation.getBook();
        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("Sách đã hết, không thể xác nhận!");
        }

        reservation.setStatus(Reservation.Status.FULFILLED);
        reservationRepository.save(reservation);

        BorrowRecord record = new BorrowRecord();
        record.setUser(reservation.getUser());
        record.setBook(book);
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(14));
        record.setStatus(BorrowRecord.Status.BORROWING);
        borrowRecordRepository.save(record);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        feedbackService.recordEvent(
                reservation.getUser().getId(),
                book.getId(),
                Feedback.EventType.BORROW);

        return reservationMapper.toDTO(reservation);
    }

    // Độc giả huỷ đặt giữ (ĐÃ SỬA: Thêm userId để chặn huỷ trộm)
    @Transactional
    public ReservationDTO cancelReservation(Long reservationId, Long currentUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đặt giữ"));

        // Lớp bảo mật: Chặn đứng nếu user đang đăng nhập cố tình huỷ sách của user khác
        if (!reservation.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Truy cập trái phép: Bạn không có quyền huỷ yêu cầu của người khác!");
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        return reservationMapper.toDTO(reservationRepository.save(reservation));
    }

    public List<ReservationDTO> getUserReservations(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ReservationDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ReservationDTO> getPendingReservations() {
        return reservationRepository.findByStatus(Reservation.Status.PENDING).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }
}