package com.library.service;

import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.Feedback;
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

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final FeedbackService feedbackService;

    // Độc giả đặt giữ sách
    @Transactional
    public Reservation createReservation(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user"));

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sách"));

        // Kiểm tra đã đặt giữ chưa
        if (reservationRepository.existsByUserIdAndBookIdAndStatus(
                userId, bookId, Reservation.Status.PENDING)) {
            throw new RuntimeException("Bạn đã đặt giữ cuốn sách này rồi!");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(Reservation.Status.PENDING);

        return reservationRepository.save(reservation);
    }

    // Thủ thư xác nhận cho mượn → đổi reservation sang FULFILLED
    @Transactional
    public Reservation fulfillReservation(Long reservationId) {
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
            Feedback.EventType.BORROW
        );

        return reservation;
    }

    // Độc giả huỷ đặt giữ
    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đặt giữ"));

        reservation.setStatus(Reservation.Status.CANCELLED);
        return reservationRepository.save(reservation);
    }

    public List<Reservation> getUserReservations(Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    // Thủ thư xem danh sách đặt giữ đang chờ
    // UI librarian/reservations cần xem tất cả trạng thái để filter:
    // PENDING, FULFILLED, CANCELLED, NOTIFIED.
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    // Dashboard chỉ cần số lượng đặt giữ đang chờ xử lý.
    public List<Reservation> getPendingReservations() {
        return reservationRepository.findByStatus(Reservation.Status.PENDING);
    }
}
