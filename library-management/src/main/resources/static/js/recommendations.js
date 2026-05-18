document.addEventListener("DOMContentLoaded", () => {
    const cards = Array.from(document.querySelectorAll(".book-list-card"));
    bindBorrowFlow(cards);
    bindBookDetailNavigation(cards);
});

function bindBorrowFlow(cards) {
    const confirmModalElement = document.getElementById("borrowConfirmModal");
    const confirmModal = new bootstrap.Modal(confirmModalElement);

    cards.forEach(card => {
        card.querySelector(".borrow-book-btn")?.addEventListener("click", () => {
            openBorrowConfirm(card, confirmModal);
        });
    });

    document.getElementById("confirm-borrow-btn")
        .addEventListener("click", borrowSelectedBook);
}

function bindBookDetailNavigation(cards) {
    cards.forEach(card => {
        card.addEventListener("click", event => {
            if (event.target.closest("button, a, input, select, textarea, .book-actions")) {
                return;
            }
            window.location.href = `/reader/books/${encodeURIComponent(card.dataset.id)}`;
        });
    });
}

let selectedBorrowCard = null;

function openBorrowConfirm(card, modal) {
    if (!card || Number(card.dataset.available || 0) <= 0) return;

    selectedBorrowCard = card;
    document.getElementById("confirm-book-title").textContent = card.dataset.title || "sách này";
    document.getElementById("confirm-borrow-date").textContent = LibraryUI.formatDate(new Date());
    document.getElementById("confirm-due-date").textContent = "Chờ thủ thư duyệt";

    const confirmButton = document.getElementById("confirm-borrow-btn");
    confirmButton.disabled = false;
    confirmButton.textContent = "Xác nhận đặt giữ";

    document.getElementById("borrow-confirm-message").textContent = "";
    modal.show();
}

async function borrowSelectedBook() {
    const card = selectedBorrowCard;
    if (!card) return;

    const confirmButton = document.getElementById("confirm-borrow-btn");
    confirmButton.disabled = true;
    confirmButton.textContent = "Đang xử lý...";

    const res = await fetch("/reservations/create", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "include",
        body: new URLSearchParams({ bookId: card.dataset.id })
    });

    const data = await LibraryUI.readJson(res);
    const msgEl = document.getElementById("borrow-confirm-message");

    if (!res.ok) {
        msgEl.textContent = data?.message || "Đặt giữ thất bại.";
        msgEl.className = "error-message mt-3";
        confirmButton.disabled = false;
        confirmButton.textContent = "Xác nhận đặt giữ";
        return;
    }

    msgEl.textContent = "Đặt giữ thành công. Vui lòng chờ thủ thư duyệt.";
    msgEl.className = "success-message mt-3";
    card.querySelector(".borrow-book-btn").disabled = true;
    card.querySelector(".borrow-book-btn").textContent = "Đã đặt giữ";
    confirmButton.textContent = "Đã đặt giữ";
}