let userId = null;
let allBooks = [];

document.addEventListener("DOMContentLoaded", async () => {
    const user = await getUser();
    if (!user) {
        showMessage("Bạn cần đăng nhập để dùng chức năng đặt giữ sách.", false);
        return;
    }

    await Promise.all([loadBooks(), loadBorrowedBooks()]);

    document.getElementById("borrow-btn").addEventListener("click", borrowBooks);
    document.getElementById("refresh-books").addEventListener("click", loadBooks);
    document.getElementById("refresh-borrowed").addEventListener("click", loadBorrowedBooks);
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
        return;
    }

    allBooks = await res.json();
    const availableBooks = Array.isArray(allBooks)
        ? allBooks.filter(book => Number(book.availableCopies) > 0)
        : [];

    if (availableBooks.length === 0) {
        container.innerHTML = "<div class='col-12'><div class='alert alert-info mb-0'>Không có sách để đặt giữ.</div></div>";
        updateSelectedCount();
        return;
    }

    renderBooks(availableBooks);
    updateSelectedCount();
}

function renderBooks(books) {
    const container = document.getElementById("books-container");
    container.innerHTML = books.map(book => `
        <div class="col-md-4 col-lg-3">
            <div class="card book-selector-card shadow-sm" data-book-id="${book.id}">
                <input type="checkbox" class="book-checkbox" value="${book.id}" data-title="${LibraryUI.escapeHtml(book.title)}" aria-label="Chọn ${LibraryUI.escapeHtml(book.title)}">
                <div class="book-image-container">
                    ${book.imageUrl ? `<img src="${book.imageUrl}" alt="${LibraryUI.escapeHtml(book.title)}">` : "<span class='fs-1'>Sách</span>"}
                </div>
                <div class="book-info">
                    <h6>${LibraryUI.escapeHtml(book.title)}</h6>
                    <p class="author">Tác giả: ${LibraryUI.escapeHtml(book.author || "Không rõ")}</p>
                    <p class="available">Còn lại: <strong>${book.availableCopies}</strong> cuốn</p>
                </div>
            </div>
        </div>
    `).join("");

    document.querySelectorAll(".book-checkbox").forEach(checkbox => {
        checkbox.addEventListener("change", event => {
            event.target.closest(".book-selector-card").classList.toggle("selected", event.target.checked);
            updateSelectedCount();
        });
    });
}

function updateSelectedCount() {
    const selected = getSelectedBooks();
    document.getElementById("selected-count").textContent = selected.length;
    document.getElementById("borrow-btn").disabled = selected.length === 0;
}

function getSelectedBooks() {
    return Array.from(document.querySelectorAll(".book-checkbox:checked"))
        .map(element => ({ id: Number(element.value), title: element.dataset.title }));
}

async function borrowBooks() {
    const selected = getSelectedBooks();
    if (selected.length === 0) {
        showMessage("Bạn cần chọn ít nhất 1 sách để đặt giữ.", false);
        return;
    }

    const failed = [];

    for (const book of selected) {
        const res = await fetch("/reservations/create", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            credentials: "include",
            body: new URLSearchParams({ bookId: book.id })
        });

        if (!res.ok) {
            failed.push(book.title);
        }
    }

    if (failed.length > 0) {
        showMessage(`Một số sách đặt giữ thất bại: ${failed.join(", ")}.`, false);
        return;
    }

    showMessage(`Đã tạo yêu cầu đặt giữ cho ${selected.length} sách. Vui lòng chờ thủ thư duyệt.`, true);
    await loadBooks();
}

async function loadBorrowedBooks() {
    const tbody = document.getElementById("borrowed-body");
    const summary = document.getElementById("return-summary");
    tbody.innerHTML = "<tr><td colspan='4'>Đang tải...</td></tr>";
    summary.innerHTML = "";

    if (!userId) {
        tbody.innerHTML = "<tr><td colspan='4'>Bạn cần đăng nhập để xem sách đang mượn.</td></tr>";
        return;
    }

    const res = await fetch(`/api/users/${userId}/borrow-history`, { credentials: "include" });
    const data = await LibraryUI.readJson(res);

    if (!res.ok) {
        tbody.innerHTML = "<tr><td colspan='4'>Lấy danh sách đang mượn thất bại.</td></tr>";
        return;
    }

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
