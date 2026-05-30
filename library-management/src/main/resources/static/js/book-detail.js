const bookReviewState = {
    currentPage: 0,
    pageSize: 4,
    totalPages: 1,
    totalElements: 0,
    reviews: [],
    currentUserId: null,
    editModal: null,
    editingOriginal: null
};

document.addEventListener("DOMContentLoaded", async () => {
    bookReviewState.editModal = new bootstrap.Modal(document.getElementById("bookReviewEditModal"));
    bindReservationButton();
    bindBookReviewForm();
    bindBookReviewEditForm();
    bindBookReviewPagination();
    await loadCurrentUser();
    await loadBookReviews();
});

function bindReservationButton() {
    const button = document.getElementById("reserve-detail-btn");
    const message = document.getElementById("book-detail-message");

    if (!button) {
        return;
    }

    button.addEventListener("click", async () => {
        button.disabled = true;
        button.textContent = "Đang xử lý...";
        message.className = "";
        message.textContent = "";

        const response = await fetch("/api/reservations", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ bookId: button.dataset.bookId })
        });

        const data = await LibraryUI.readJson(response);

        if (!response.ok) {
            message.className = "error-message mt-3";
            message.textContent = data?.message || "Đặt giữ thất bại.";
            button.disabled = false;
            button.textContent = "Đặt giữ sách";
            return;
        }

        message.className = "success-message mt-3";
        message.textContent = "Đặt giữ thành công. Vui lòng chờ thủ thư duyệt.";
        button.textContent = "Đã đặt giữ";
    });
}

function bindBookReviewForm() {
    document.getElementById("book-review-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        await submitBookReview();
    });
}

function bindBookReviewEditForm() {
    document.getElementById("book-review-edit-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        await submitBookReviewEdit();
    });
}

function bindBookReviewPagination() {
    document.getElementById("book-reviews-prev")?.addEventListener("click", () => {
        if (bookReviewState.currentPage > 0) {
            loadBookReviews(bookReviewState.currentPage - 1);
        }
    });

    document.getElementById("book-reviews-next")?.addEventListener("click", () => {
        if (bookReviewState.currentPage < bookReviewState.totalPages - 1) {
            loadBookReviews(bookReviewState.currentPage + 1);
        }
    });
}

async function loadCurrentUser() {
    try {
        const response = await fetch("/api/users/me", { credentials: "include" });
        const user = await LibraryUI.readJson(response);

        if (response.ok && user?.id) {
            bookReviewState.currentUserId = user.id;
        }
    } catch (error) {
        bookReviewState.currentUserId = null;
    }
}

async function loadBookReviews(page = bookReviewState.currentPage) {
    const section = document.querySelector(".book-reviews-section");
    const list = document.getElementById("book-reviews-list");
    const averageScore = document.getElementById("book-average-score");
    const reviewCount = document.getElementById("book-review-count");

    if (!section || !list) {
        return;
    }

    try {
        const bookId = encodeURIComponent(section.dataset.bookId);
        const [reviewResponse, averageResponse] = await Promise.all([
            fetch(`/api/feedbacks/books/${bookId}?page=${page}&size=${bookReviewState.pageSize}&sortDir=desc`, {
                credentials: "include"
            }),
            fetch(`/api/feedbacks/books/${bookId}/average-score`, {
                credentials: "include"
            })
        ]);

        const reviewData = await LibraryUI.readJson(reviewResponse);
        const averageData = await LibraryUI.readJson(averageResponse);

        if (!reviewResponse.ok || !Array.isArray(reviewData.content)) {
            throw new Error(reviewData?.message || "Không tải được đánh giá.");
        }

        bookReviewState.reviews = reviewData.content;
        bookReviewState.currentPage = reviewData.number || 0;
        bookReviewState.totalPages = reviewData.totalPages || 1;
        bookReviewState.totalElements = reviewData.totalElements || 0;

        const average = Number(averageData?.averageScore || 0);
        averageScore.textContent = average > 0 ? average.toFixed(1) : "-";
        reviewCount.textContent = `${bookReviewState.totalElements} đánh giá`;

        renderBookReviews(reviewData.content);
        renderBookReviewPagination();
    } catch (error) {
        averageScore.textContent = "-";
        reviewCount.textContent = "0 đánh giá";
        bookReviewState.reviews = [];
        bookReviewState.currentPage = 0;
        bookReviewState.totalPages = 1;
        bookReviewState.totalElements = 0;
        list.innerHTML = `<div class="book-reviews-empty">${LibraryUI.escapeHtml(error.message)}</div>`;
        renderBookReviewPagination();
    }
}

async function submitBookReview() {
    const section = document.querySelector(".book-reviews-section");
    const submitButton = document.getElementById("book-review-submit");
    const score = Number(document.getElementById("book-review-score").value);
    const comment = document.getElementById("book-review-comment").value.trim();

    if (!section) {
        return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "Đang gửi...";

    try {
        const response = await fetch("/api/feedbacks", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({
                bookId: Number(section.dataset.bookId),
                score,
                comment
            })
        });
        const data = await LibraryUI.readJson(response);

        if (!response.ok) {
            throw new Error(data?.message || "Gửi đánh giá thất bại.");
        }

        document.getElementById("book-review-form").reset();
        document.getElementById("book-review-score").value = "5";
        notifyBookReview("Gửi đánh giá thành công.", true);
        await loadBookReviews(0);
    } catch (error) {
        notifyBookReview(error.message, false);
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "Gửi đánh giá";
    }
}

function renderBookReviews(reviews) {
    const list = document.getElementById("book-reviews-list");

    if (!reviews.length) {
        list.innerHTML = '<div class="book-reviews-empty">Chưa có đánh giá nào cho cuốn sách này.</div>';
        return;
    }

    list.innerHTML = reviews.map(item => {
        const score = Number(item.score || 0);
        const displayName = LibraryUI.escapeHtml(item.fullName || item.username || "Độc giả");
        const comment = LibraryUI.escapeHtml(item.comment || "");
        const date = LibraryUI.formatDate(item.eventDate);
        const initial = LibraryUI.escapeHtml(getReviewerInitial(item.fullName || item.username));
        const ownActions = isOwnReview(item) ? `
            <div class="book-review-actions d-flex justify-content-end gap-2 mt-3">
                <button class="btn btn-outline-primary btn-sm book-review-edit-btn" type="button" data-review-id="${item.id}">Sửa</button>
                <button class="btn btn-outline-danger btn-sm book-review-delete-btn" type="button" data-review-id="${item.id}">Xóa</button>
            </div>
        ` : "";

        return `
            <article class="book-review-item">
                <div class="book-review-topline">
                    <div class="book-review-user">
                        <span class="book-review-avatar">${initial}</span>
                        <div>
                            <strong>${displayName}</strong>
                            <span>${date}</span>
                        </div>
                    </div>
                    <div class="book-review-meta">
                        <div class="book-review-score">
                            <span class="book-review-stars">${renderReviewStars(score)}</span>
                            <span>${score || "-"}/5</span>
                        </div>
                    </div>
                </div>
                <p class="${comment ? "" : "is-empty"}">${comment || "Không có bình luận."}</p>
                ${ownActions}
            </article>
        `;
    }).join("");

    bindBookReviewActions();
}

function bindBookReviewActions() {
    document.querySelectorAll(".book-review-edit-btn").forEach(button => {
        button.addEventListener("click", () => openBookReviewEdit(button.dataset.reviewId));
    });

    document.querySelectorAll(".book-review-delete-btn").forEach(button => {
        button.addEventListener("click", () => confirmDeleteBookReview(button.dataset.reviewId));
    });
}

function openBookReviewEdit(reviewId) {
    const review = findReviewById(reviewId);

    if (!review) {
        notifyBookReview("Không tìm thấy đánh giá cần sửa.", false);
        return;
    }

    document.getElementById("edit-book-review-id").value = review.id;
    document.getElementById("edit-book-review-score").value = review.score || 5;
    document.getElementById("edit-book-review-comment").value = review.comment || "";
    bookReviewState.editingOriginal = {
        score: Number(review.score || 5),
        comment: String(review.comment || "").trim()
    };
    bookReviewState.editModal.show();
}

async function submitBookReviewEdit() {
    const id = document.getElementById("edit-book-review-id").value;
    const score = Number(document.getElementById("edit-book-review-score").value);
    const comment = document.getElementById("edit-book-review-comment").value.trim();
    const button = document.getElementById("book-review-edit-submit");
    const original = bookReviewState.editingOriginal;

    if (original && original.score === score && original.comment === comment) {
        notifyBookReview("Bạn chưa thay đổi nội dung đánh giá.", false);
        return;
    }

    button.disabled = true;
    button.textContent = "Đang lưu...";

    try {
        const response = await fetch(`/api/feedbacks/${encodeURIComponent(id)}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ score, comment })
        });
        const data = await LibraryUI.readJson(response);

        if (!response.ok) {
            throw new Error(data?.message || "Cập nhật đánh giá thất bại.");
        }

        bookReviewState.editModal.hide();
        notifyBookReview("Cập nhật đánh giá thành công.", true);
        await loadBookReviews(bookReviewState.currentPage);
    } catch (error) {
        notifyBookReview(error.message, false);
    } finally {
        button.disabled = false;
        button.textContent = "Lưu thay đổi";
    }
}

async function confirmDeleteBookReview(reviewId) {
    const result = await Swal.fire({
        icon: "warning",
        title: "Xóa đánh giá?",
        text: "Đánh giá đã xóa sẽ không thể khôi phục.",
        showCancelButton: true,
        confirmButtonText: "Xóa",
        cancelButtonText: "Hủy",
        confirmButtonColor: "#dc3545"
    });

    if (!result.isConfirmed) {
        return;
    }

    await deleteBookReview(reviewId);
}

async function deleteBookReview(reviewId) {
    try {
        const response = await fetch(`/api/feedbacks/${encodeURIComponent(reviewId)}`, {
            method: "DELETE",
            credentials: "include"
        });

        if (!response.ok) {
            const data = await LibraryUI.readJson(response);
            throw new Error(data?.message || "Xóa đánh giá thất bại.");
        }

        const nextPage = bookReviewState.reviews.length === 1 && bookReviewState.currentPage > 0
            ? bookReviewState.currentPage - 1
            : bookReviewState.currentPage;

        notifyBookReview("Xóa đánh giá thành công.", true);
        await loadBookReviews(nextPage);
    } catch (error) {
        notifyBookReview(error.message, false);
    }
}

function findReviewById(reviewId) {
    return bookReviewState.reviews.find(item => String(item.id) === String(reviewId));
}

function isOwnReview(review) {
    return bookReviewState.currentUserId != null
        && String(review.userId) === String(bookReviewState.currentUserId);
}

function getReviewerInitial(username) {
    return String(username || "Đ")
        .trim()
        .charAt(0)
        .toUpperCase();
}

function renderBookReviewPagination() {
    const pagination = document.getElementById("book-reviews-pagination");
    const pageInfo = document.getElementById("book-reviews-page-info");
    const prevButton = document.getElementById("book-reviews-prev");
    const nextButton = document.getElementById("book-reviews-next");

    if (!pagination || !pageInfo || !prevButton || !nextButton) {
        return;
    }

    pagination.hidden = bookReviewState.totalPages <= 1;
    pageInfo.textContent = `Trang ${bookReviewState.currentPage + 1} / ${bookReviewState.totalPages}`;
    prevButton.disabled = bookReviewState.currentPage <= 0;
    nextButton.disabled = bookReviewState.currentPage >= bookReviewState.totalPages - 1;
}

function renderReviewStars(score) {
    const filled = Math.max(1, Math.min(5, Math.round(score || 1)));
    return "&#9733;".repeat(filled) + "&#9734;".repeat(5 - filled);
}

function notifyBookReview(message, success) {
    Swal.fire({
        icon: success ? "success" : "error",
        title: success ? "Thành công" : "Lỗi",
        text: message,
        timer: 2000,
        timerProgressBar: true,
        showConfirmButton: false
    });
}
