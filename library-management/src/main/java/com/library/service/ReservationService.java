package com.library.service;

import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.ReservationRepository;
import com.library.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

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

        reservation.setStatus(Reservation.Status.FULFILLED);
        return reservationRepository.save(reservation);
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
    public List<Reservation> getPendingReservations() {
        return reservationRepository.findByStatus(Reservation.Status.PENDING);
    }
}