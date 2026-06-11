const booksState = {
    currentPage: 1,
    pageSize: 8,
    filteredCards: [],
    selectedBorrowCard: null,
    currentBookState: new Map()
};

document.addEventListener("DOMContentLoaded", async () => {
    const cards = Array.from(document.querySelectorAll(".book-list-card"));
    booksState.filteredCards = cards;

    booksState.currentBookState = await loadCurrentBookState();
    applyBookStateBadges(cards);
    buildCategoryFilter(cards);
    bindFilters();
    bindBorrowFlow(cards);
    bindBookDetailNavigation(cards);
    bindPagination();
    syncFilterBarWithCarousel();
    window.addEventListener("resize", syncFilterBarWithCarousel);
    applyFilters();
});

async function loadCurrentBookState() {
    const state = new Map();
    const [borrowRecords, reservations] = await Promise.all([
        fetchJsonSafe("/api/borrow/me"),
        fetchJsonSafe("/api/reservations/me")
    ]);

    if (Array.isArray(borrowRecords)) {
        borrowRecords
            .filter(record => record.status !== "RETURNED")
            .forEach(record => {
                if (record.bookId) {
                    state.set(Number(record.bookId), {
                        className: "borrowed",
                        label: "\u0110ang m\u01b0\u1ee3n"
                    });
                }
            });
    }

    if (Array.isArray(reservations)) {
        reservations
            .filter(reservation => reservation.status === "PENDING")
            .forEach(reservation => {
                const bookId = Number(reservation.bookId);
                if (bookId && !state.has(bookId)) {
                    state.set(bookId, {
                        className: "pending",
                        label: "Ch\u1edd duy\u1ec7t"
                    });
                }
            });
    }

    return state;
}

async function fetchJsonSafe(url) {
    try {
        const response = await fetch(url, { credentials: "include" });
        if (!response.ok) return [];
        return await LibraryUI.readJson(response);
    } catch (error) {
        console.error(`Kh\u00f4ng l\u1ea5y \u0111\u01b0\u1ee3c d\u1eef li\u1ec7u t\u1eeb ${url}`, error);
        return [];
    }
}

function applyBookStateBadges(cards) {
    cards.forEach(card => {
        const state = booksState.currentBookState.get(Number(card.dataset.id));
        const button = card.querySelector(".borrow-book-btn");
        card.querySelector(".book-state-badge")?.remove();
        card.classList.toggle("has-user-state", Boolean(state));

        if (!state) {
            if (button && Number(card.dataset.available || 0) > 0) {
                button.disabled = false;
                button.textContent = "\u0110\u1eb7t gi\u1eef";
            }
            return;
        }

        const badge = document.createElement("span");
        badge.className = `book-state-badge ${state.className}`;
        badge.textContent = state.label;
        card.querySelector(".book-cover-wrap")?.appendChild(badge);

        if (button) {
            button.disabled = true;
            button.textContent = state.label;
        }
    });
}

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

    if (booksState.currentBookState.has(Number(card.dataset.id))) {
        return;
    }

    const availableCopies = Number(card.dataset.available || 0);
    if (availableCopies <= 0) {
        return;
    }

    booksState.selectedBorrowCard = card;

    const reservationDate = new Date();

    document.getElementById("confirm-book-title").textContent = card.dataset.title || "s\u00e1ch n\u00e0y";
    document.getElementById("confirm-borrow-date").textContent = LibraryUI.formatDate(reservationDate);
    document.getElementById("confirm-due-date").textContent = "Ch\u1edd th\u1ee7 th\u01b0 duy\u1ec7t";
    clearBorrowConfirmMessage();

    const confirmButton = document.getElementById("confirm-borrow-btn");
    confirmButton.disabled = false;
    confirmButton.textContent = "X\u00e1c nh\u1eadn \u0111\u1eb7t gi\u1eef";

    modal.show();
}

async function borrowSelectedBook() {
    const card = booksState.selectedBorrowCard;
    if (!card) {
        return;
    }

    const confirmButton = document.getElementById("confirm-borrow-btn");
    confirmButton.disabled = true;
    confirmButton.textContent = "\u0110ang x\u1eed l\u00fd...";
    clearBorrowConfirmMessage();

    const res = await fetch("/api/reservations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({ bookId: Number(card.dataset.id) })
    });

    const data = await LibraryUI.readJson(res);

    if (!res.ok) {
        showBorrowConfirmMessage(data?.message || "\u0110\u1eb7t gi\u1eef th\u1ea5t b\u1ea1i.", false);
        confirmButton.disabled = false;
        confirmButton.textContent = "X\u00e1c nh\u1eadn \u0111\u1eb7t gi\u1eef";
        return;
    }

    showBorrowConfirmMessage("\u0110\u1eb7t gi\u1eef th\u00e0nh c\u00f4ng. Vui l\u00f2ng ch\u1edd th\u1ee7 th\u01b0 duy\u1ec7t.", true);
    markCardAfterReservation(card);

    confirmButton.textContent = "\u0110\u00e3 \u0111\u1eb7t gi\u1eef";
}
function markCardAfterReservation(card) {
    booksState.currentBookState.set(Number(card.dataset.id), {
        className: "pending",
        label: "Ch\u1edd duy\u1ec7t"
    });
    applyBookStateBadges([card]);

    const borrowButton = card.querySelector(".borrow-book-btn");
    if (borrowButton) {
        borrowButton.disabled = true;
        borrowButton.textContent = "Ch\u1edd duy\u1ec7t";
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

