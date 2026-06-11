let userId = null;

const MAX_PENDING_RESERVATIONS = 5;
let pendingReservationCount = 0;
let currentBookState = new Map();

document.addEventListener("DOMContentLoaded", async () => {
    const user = await getUser();
    if (!user) {
        showMessage("Bạn cần đăng nhập để dùng chức năng đặt giữ sách.", false);
        return;
    }

    currentBookState = await loadCurrentBookState();
    await Promise.all([loadBooks(), loadBorrowedBooks()]);

    document.getElementById("borrow-btn").addEventListener("click", borrowBooks);
    document.getElementById("refresh-books").addEventListener("click", async () => {
        currentBookState = await loadCurrentBookState();
        await loadBooks();
    });
    document.getElementById("refresh-borrowed").addEventListener("click", async () => {
        await loadBorrowedBooks();
        currentBookState = await loadCurrentBookState();
        await loadBooks();
    });
    document.getElementById("borrow-tab").addEventListener("click", () => switchTab("borrow"));
    document.getElementById("return-tab").addEventListener("click", () => switchTab("return"));
});

async function getUser() {
    const res = await fetch("/api/users/me", { credentials: "include" });
    if (!res.ok) {
        console.error("Không lấy được user hiện tại.");
        return null;
    }

    const user = await res.json();
    userId = user.id;
    return user;
}

function switchTab(tab) {
    const borrowActive = tab === "borrow";
    document.getElementById("borrow-section").classList.toggle("is-hidden", !borrowActive);
    document.getElementById("return-section").classList.toggle("is-hidden", borrowActive);
    document.getElementById("borrow-tab").className = borrowActive ? "btn btn-primary" : "btn btn-outline-primary";
    document.getElementById("return-tab").className = borrowActive ? "btn btn-outline-primary" : "btn btn-primary";
    hideMessage();
}

async function loadBooks() {
    const container = document.getElementById("books-container");
    container.innerHTML = loadingMarkup("Đang tải sách...");

    const res = await fetch("/api/books/available", { credentials: "include" });

    if (!res.ok) {
        showMessage("Lấy sách thất bại.", false);
        container.innerHTML = "";
        updateSelectedCount();
        return;
    }

    const books = await res.json();
    const availableBooks = Array.isArray(books)
        ? books.filter(book => Number(book.availableCopies) > 0)
        : [];

    if (availableBooks.length === 0) {
        container.innerHTML = "<div class='col-12'><div class='alert alert-info mb-0'>Không có sách để đặt giữ.</div></div>";
        updateSelectedCount();
        return;
    }

    renderBooks(availableBooks);
    updateSelectedCount();
}

async function loadCurrentBookState() {
    const state = new Map();
    pendingReservationCount = 0;

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
                        label: "đang mượn"
                    });
                }
            });
    }

    if (Array.isArray(reservations)) {
        const pendingReservations = reservations.filter(reservation => reservation.status === "PENDING");
        pendingReservationCount = pendingReservations.length;

        pendingReservations.forEach(reservation => {
            const bookId = Number(reservation.bookId);
            if (bookId && !state.has(bookId)) {
                state.set(bookId, {
                    className: "pending",
                    label: "chờ duyệt"
                });
            }
        });
    }

    return state;
}

async function fetchJsonSafe(url) {
    try {
        const res = await fetch(url, { credentials: "include" });
        if (!res.ok) return [];
        return await LibraryUI.readJson(res);
    } catch (error) {
        console.error(`Không lấy được dữ liệu từ ${url}`, error);
        return [];
    }
}

function renderBooks(books) {
    const container = document.getElementById("books-container");
    container.innerHTML = books.map(book => {
        const bookState = currentBookState.get(Number(book.id));
        const disabled = Boolean(bookState) || getRemainingReservationSlots() === 0;
        const title = LibraryUI.escapeHtml(book.title);

        return `
        <div class="col-md-4 col-lg-3">
            <div class="card book-selector-card shadow-sm ${disabled ? "is-unavailable" : ""}" data-book-id="${book.id}" data-title="${title}">
                ${bookState ? `<span class="book-state-badge ${bookState.className}">${formatBookStateLabel(bookState.label)}</span>` : ""}
                <input type="checkbox" class="book-checkbox" value="${book.id}" data-title="${title}" aria-label="Chọn ${title}" ${disabled ? "disabled" : ""}>
                <div class="book-image-container">
                    ${book.imageUrl ? `<img src="${book.imageUrl}" alt="${title}">` : "<span class='fs-1'>Sách</span>"}
                </div>
                <div class="book-info">
                    <h6>${title}</h6>
                    <p class="author">Tác giả: ${LibraryUI.escapeHtml(book.author || "Không rõ")}</p>
                    <p class="available">Còn lại: <strong>${book.availableCopies}</strong> cuốn</p>
                </div>
            </div>
        </div>
    `;
    }).join("");

    document.querySelectorAll(".book-selector-card").forEach(card => {
        card.addEventListener("click", event => {
            const checkbox = card.querySelector(".book-checkbox");
            const bookId = Number(card.dataset.bookId);
            const bookState = currentBookState.get(bookId);

            if (bookState) {
                showMessage(`Sách "${card.dataset.title}" hiện đang ${bookState.label}.`, false);
                return;
            }

            if (getRemainingReservationSlots() === 0) {
                showMessage("Bạn chỉ có thể đặt giữ thêm 0 sách.", false);
                return;
            }

            if (!event.target.closest(".book-checkbox") && checkbox && !checkbox.disabled) {
                checkbox.checked = !checkbox.checked;
                checkbox.dispatchEvent(new Event("change", { bubbles: true }));
            }
        });
    });

    document.querySelectorAll(".book-checkbox").forEach(checkbox => {
        checkbox.addEventListener("click", event => event.stopPropagation());
        checkbox.addEventListener("change", event => {
            const selectedCount = getSelectedBooks().length;
            const remainingSlots = getRemainingReservationSlots();

            if (event.target.checked && selectedCount > remainingSlots) {
                event.target.checked = false;
                showMessage(`Bạn chỉ có thể đặt giữ thêm ${remainingSlots} sách.`, false);
            }

            event.target.closest(".book-selector-card").classList.toggle("selected", event.target.checked);
            updateSelectedCount();
        });
    });
}

function updateSelectedCount() {
    const selected = getSelectedBooks();
    const remainingSlots = getRemainingReservationSlots();

    document.getElementById("selected-count").textContent = selected.length;
    document.getElementById("pending-reservation-count").textContent = pendingReservationCount;
    document.getElementById("remaining-reservation-count").textContent = remainingSlots;
    document.getElementById("borrow-btn").disabled = selected.length === 0 || selected.length > remainingSlots;

    syncSelectionAvailability(selected.length, remainingSlots);
}

function getSelectedBooks() {
    return Array.from(document.querySelectorAll(".book-checkbox:checked"))
        .map(element => ({ id: Number(element.value), title: element.dataset.title }));
}

function getRemainingReservationSlots() {
    return Math.max(0, MAX_PENDING_RESERVATIONS - pendingReservationCount);
}

function syncSelectionAvailability(selectedCount, remainingSlots) {
    document.querySelectorAll(".book-checkbox").forEach(checkbox => {
        const bookId = Number(checkbox.value);
        const alreadyUnavailable = currentBookState.has(bookId);
        checkbox.disabled = alreadyUnavailable || remainingSlots === 0;
    });
}

async function borrowBooks() {
    const selected = getSelectedBooks();
    if (selected.length === 0) {
        showMessage("Bạn cần chọn ít nhất 1 sách để đặt giữ.", false);
        return;
    }

    const remainingSlots = getRemainingReservationSlots();
    if (selected.length > remainingSlots) {
        showMessage(`Bạn chỉ có thể đặt giữ thêm ${remainingSlots} sách.`, false);
        return;
    }

    const res = await fetch("/api/reservations/batch", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({ bookIds: selected.map(book => Number(book.id)) })
    });

    if (!res.ok) {
        const data = await LibraryUI.readJson(res);
        showMessage(data?.message || "Đặt giữ sách thất bại.", false);
        return;
    }

    showMessage(`Đã tạo yêu cầu đặt giữ cho ${selected.length} sách. Vui lòng chờ thủ thư duyệt.`, true);
    currentBookState = await loadCurrentBookState();
    await loadBooks();
    await loadBorrowedBooks();
}

async function loadBorrowedBooks() {
    const tbody = document.getElementById("borrowed-body");
    const summary = document.getElementById("return-summary");
    tbody.innerHTML = "<tr><td colspan='4'>Đang tải...</td></tr>";
    summary.innerHTML = "";

    const res = await fetch("/api/borrow/me", { credentials: "include" });

    if (res.status === 401 || res.status === 403) {
        tbody.innerHTML = "<tr><td colspan='4'>Bạn cần đăng nhập để xem sách đang mượn.</td></tr>";
        return;
    }

    if (!res.ok) {
        tbody.innerHTML = "<tr><td colspan='4'>Lấy danh sách đang mượn thất bại.</td></tr>";
        return;
    }

    const data = await LibraryUI.readJson(res);

    const activeRecords = Array.isArray(data)
        ? data.filter(record => record.status !== "RETURNED")
        : [];

    renderBorrowedBooks(activeRecords);
}

function renderBorrowedBooks(records) {
    const tbody = document.getElementById("borrowed-body");
    const summary = document.getElementById("return-summary");
    const overdueCount = records.filter(record => isOverdue(record.status, record.dueDate)).length;

    document.getElementById("current-count").textContent = records.length;
    summary.innerHTML = `
        <div class="summary-item">
            <span>Đang mượn</span>
            <strong>${records.length}</strong>
        </div>
        <div class="summary-item">
            <span>Quá hạn</span>
            <strong class="${overdueCount > 0 ? "status-overdue" : ""}">${overdueCount}</strong>
        </div>
    `;

    if (records.length === 0) {
        tbody.innerHTML = "<tr><td colspan='4' class='text-center text-muted'>Bạn không có sách nào đang mượn.</td></tr>";
        return;
    }

    tbody.innerHTML = records.map(record => `
        <tr>
            <td><strong>${LibraryUI.escapeHtml(record.bookTitle)}</strong></td>
            <td>${LibraryUI.formatDate(record.borrowDate)}</td>
            <td>${LibraryUI.formatDate(record.dueDate)}</td>
            <td><span class="${statusClass(record.status, record.dueDate)}">${formatStatus(record.status, record.dueDate)}</span></td>
        </tr>
    `).join("");
}

function showMessage(message, success) {
    const element = document.getElementById("borrow-message");
    element.textContent = "";
    element.className = "";

    if (window.Swal) {
        Swal.fire({
            toast: true,
            position: "top-end",
            icon: success ? "success" : "warning",
            title: message,
            showConfirmButton: false,
            timer: 1500,
            timerProgressBar: true
        });
        return;
    }

    element.textContent = message;
    element.className = success ? "success-message" : "error-message";
}

function hideMessage() {
    const element = document.getElementById("borrow-message");
    element.textContent = "";
    element.className = "";
}

function loadingMarkup(message) {
    return `
        <div class="col-12 loading-placeholder">
            <div class="text-center">
                <div class="spinner-border text-primary mb-3"></div>
                <p>${message}</p>
            </div>
        </div>
    `;
}

function formatBookStateLabel(label) {
    const labels = {
        "đang mượn": "Đang mượn",
        "chờ duyệt": "Chờ duyệt"
    };
    return labels[label] || label;
}

function formatStatus(status, dueDate) {
    if (isOverdue(status, dueDate)) return "QUÁ HẠN";
    if (status === "BORROWING") return "ĐANG MƯỢN";
    if (status === "RETURNED") return "ĐÃ TRẢ";
    return status;
}

function statusClass(status, dueDate) {
    if (isOverdue(status, dueDate)) return "status-overdue";
    if (status === "RETURNED") return "status-returned";
    return "status-borrowing";
}

function isOverdue(status, dueDate) {
    if (!dueDate) return false;
    return (status === "OVERDUE") || (status === "BORROWING" && new Date(dueDate) < new Date());
}
