const booksState = {
    currentPage: 1,
    pageSize: 8,
    filteredCards: [],
    selectedBorrowCard: null
};

document.addEventListener("DOMContentLoaded", () => {
    const cards = Array.from(document.querySelectorAll(".book-list-card"));
    booksState.filteredCards = cards;

    buildCategoryFilter(cards);
    bindFilters();
    bindBorrowFlow(cards);
    bindBookDetailNavigation(cards);
    bindPagination();
    syncFilterBarWithCarousel();
    window.addEventListener("resize", syncFilterBarWithCarousel);
    applyFilters();
});

function buildCategoryFilter(cards) {
    const select = document.getElementById("category-filter");
    const categories = [...new Set(cards.map(card => card.dataset.category).filter(Boolean))].sort();

    categories.forEach(category => {
        const option = document.createElement("option");
        option.value = category;
        option.textContent = category;
        select.appendChild(option);
    });
}

function bindFilters() {
    ["book-search", "category-filter", "availability-filter"].forEach(id => {
        document.getElementById(id).addEventListener("input", () => {
            booksState.currentPage = 1;
            applyFilters();
        });
    });
}

function bindPagination() {
    document.getElementById("prev-page").addEventListener("click", () => {
        if (booksState.currentPage > 1) {
            booksState.currentPage--;
            renderPage();
        }
    });

    document.getElementById("next-page").addEventListener("click", () => {
        const totalPages = getTotalPages();
        if (booksState.currentPage < totalPages) {
            booksState.currentPage++;
            renderPage();
        }
    });
}

function bindBorrowFlow(cards) {
    const confirmModalElement = document.getElementById("borrowConfirmModal");
    const confirmModal = new bootstrap.Modal(confirmModalElement);

    cards.forEach(card => {
        card.querySelector(".borrow-book-btn")?.addEventListener("click", () => {
            openBorrowConfirm(card, confirmModal);
        });
    });

    document.getElementById("confirm-borrow-btn").addEventListener("click", borrowSelectedBook);
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

function syncFilterBarWithCarousel() {
    const carousel = document.getElementById("topBooksCarousel");
    const filterBar = document.querySelector(".filter-bar");

    if (!carousel || !filterBar) {
        return;
    }

    const rect = carousel.getBoundingClientRect();
    filterBar.style.left = `${rect.left}px`;
    filterBar.style.width = `${rect.width}px`;
    filterBar.style.transform = "none";
}

function applyFilters() {
    const cards = Array.from(document.querySelectorAll(".book-list-card"));
    const keyword = LibraryUI.normalizeText(document.getElementById("book-search").value);
    const category = document.getElementById("category-filter").value;
    const availability = document.getElementById("availability-filter").value;

    booksState.filteredCards = cards.filter(card => {
        const searchable = LibraryUI.normalizeText(`${card.dataset.title} ${card.dataset.author}`);
        const matchesKeyword = searchable.includes(keyword);
        const matchesCategory = !category || card.dataset.category === category;
        const availableCopies = Number(card.dataset.available || 0);
        const matchesAvailability =
            !availability ||
            (availability === "available" && availableCopies > 0) ||
            (availability === "unavailable" && availableCopies <= 0);

        return matchesKeyword && matchesCategory && matchesAvailability;
    });

    renderPage();
}

function renderPage() {
    const cards = Array.from(document.querySelectorAll(".book-list-card"));
    const totalPages = getTotalPages();
    const start = (booksState.currentPage - 1) * booksState.pageSize;
    const end = start + booksState.pageSize;
    const visibleCards = booksState.filteredCards.slice(start, end);

    cards.forEach(card => card.classList.add("is-hidden"));
    visibleCards.forEach(card => card.classList.remove("is-hidden"));

    document.getElementById("empty-state").classList.toggle("is-hidden", booksState.filteredCards.length > 0);
    document.getElementById("page-info").textContent = booksState.filteredCards.length
        ? `Trang ${booksState.currentPage} / ${totalPages}`
        : "Trang 0 / 0";

    document.getElementById("prev-page").disabled = booksState.currentPage <= 1;
    document.getElementById("next-page").disabled = booksState.currentPage >= totalPages;
}

function getTotalPages() {
    return Math.max(1, Math.ceil(booksState.filteredCards.length / booksState.pageSize));
}

function openBorrowConfirm(card, modal) {
    if (!card) {
        return;
    }

    const availableCopies = Number(card.dataset.available || 0);
    if (availableCopies <= 0) {
        return;
    }

    booksState.selectedBorrowCard = card;

    const reservationDate = new Date();

    document.getElementById("confirm-book-title").textContent = card.dataset.title || "sách này";
    document.getElementById("confirm-borrow-date").textContent = LibraryUI.formatDate(reservationDate);
    document.getElementById("confirm-due-date").textContent = "Chờ thủ thư duyệt";
    clearBorrowConfirmMessage();

    const confirmButton = document.getElementById("confirm-borrow-btn");
    confirmButton.disabled = false;
    confirmButton.textContent = "Xác nhận đặt giữ";

    modal.show();
}

async function borrowSelectedBook() {
    const card = booksState.selectedBorrowCard;
    if (!card) {
        return;
    }

    const confirmButton = document.getElementById("confirm-borrow-btn");
    confirmButton.disabled = true;
    confirmButton.textContent = "Đang xử lý...";
    clearBorrowConfirmMessage();

    const res = await fetch("/reservations/create", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "include",
        body: new URLSearchParams({ bookId: card.dataset.id })
    });

    const data = await LibraryUI.readJson(res);

    if (!res.ok) {
        showBorrowConfirmMessage(data?.message || "Đặt giữ thất bại.", false);
        confirmButton.disabled = false;
        confirmButton.textContent = "Xác nhận đặt giữ";
        return;
    }

    showBorrowConfirmMessage("Đặt giữ thành công. Vui lòng chờ thủ thư duyệt.", true);
    markCardAfterReservation(card);

    confirmButton.textContent = "Đã đặt giữ";
}

function markCardAfterReservation(card) {
    const borrowButton = card.querySelector(".borrow-book-btn");
    if (borrowButton) {
        borrowButton.disabled = true;
        borrowButton.textContent = "Đã đặt giữ";
    }
}

function showBorrowConfirmMessage(message, success) {
    const element = document.getElementById("borrow-confirm-message");
    element.textContent = message;
    element.className = success ? "success-message mt-3" : "error-message mt-3";
}

function clearBorrowConfirmMessage() {
    const element = document.getElementById("borrow-confirm-message");
    element.textContent = "";
    element.className = "mt-3";
}
