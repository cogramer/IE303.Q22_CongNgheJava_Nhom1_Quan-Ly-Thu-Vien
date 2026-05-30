const HISTORY_STATUS = {
    BORROWING: { className: "borrowing", label: "Đang mượn" },
    OVERDUE: { className: "late", label: "Quá hạn" },
    PENDING: { className: "pending", label: "Chờ duyệt" },
    RETURNED: { className: "on-time", label: "Đã trả" },
    "on-time": { className: "on-time", label: "Trả đúng hạn" },
    late: { className: "late", label: "Trả trễ hạn" }
};

document.addEventListener("DOMContentLoaded", () => {
    bindHistoryFilters();
    Promise.all([loadBorrowHistory(), loadPendingReservations()]);
});

function bindHistoryFilters() {
    const fields = [
        document.getElementById("history-search"),
        document.getElementById("history-month"),
        document.getElementById("history-status")
    ];

    fields.forEach(field => {
        field?.addEventListener("input", applyHistoryFilters);
        field?.addEventListener("change", applyHistoryFilters);
    });

    document.getElementById("history-clear-filter")?.addEventListener("click", () => {
        fields.forEach(field => {
            if (field) field.value = "";
        });
        applyHistoryFilters();
    });
}

async function loadBorrowHistory() {
    try {
        const records = await fetchJson("/api/borrow/me");
        renderHistoryRows(Array.isArray(records) ? records : []);
        updateHistoryStats(Array.isArray(records) ? records : []);
        applyHistoryFilters();
    } catch (error) {
        console.warn("Không tải được API lịch sử mượn, dùng dữ liệu server render.", error);
        applyHistoryFilters();
    }
}

async function loadPendingReservations() {
    const container = document.getElementById("pending-reservations");
    if (!container) return;

    container.innerHTML = emptyMarkup("bi-hourglass-split", "Đang tải yêu cầu đặt giữ...");

    try {
        const reservations = await fetchJson("/api/reservations/me");
        const pending = Array.isArray(reservations)
            ? reservations.filter(reservation => reservation.status === "PENDING")
            : [];

        renderPendingReservations(pending);
    } catch (error) {
        console.warn("Không tải được yêu cầu đặt giữ.", error);
        LibraryUI.setText("pending-reservation-count", 0);
        container.innerHTML = emptyMarkup("bi-exclamation-circle", "Không tải được yêu cầu đang chờ duyệt.");
    }
}

function renderPendingReservations(reservations) {
    const container = document.getElementById("pending-reservations");
    if (!container) return;

    LibraryUI.setText("pending-reservation-count", reservations.length);

    if (!reservations.length) {
        container.innerHTML = emptyMarkup("bi-check2-circle", "Không có yêu cầu nào đang chờ duyệt.");
        return;
    }

    container.innerHTML = reservations.map(reservation => `
        <article class="pending-reservation-item">
            <div>
                <strong>${LibraryUI.escapeHtml(reservation.bookTitle || "Không có tên sách")}</strong>
                <span>Ngày đặt: ${LibraryUI.formatDate(reservation.reservationDate)}</span>
            </div>
            <div class="pending-reservation-actions">
                ${statusBadge("PENDING")}
                <button class="btn btn-outline-danger btn-sm cancel-reservation-btn" type="button" data-id="${reservation.id}">
                    Hủy
                </button>
            </div>
        </article>
    `).join("");

    container.querySelectorAll(".cancel-reservation-btn").forEach(button => {
        button.addEventListener("click", () => cancelReservation(button));
    });
}

async function cancelReservation(button) {
    const reservationId = button.dataset.id;
    if (!reservationId) return;

    button.disabled = true;
    button.textContent = "Đang hủy...";

    try {
        const response = await fetch(`/api/reservations/${reservationId}/cancel`, {
            method: "PUT",
            credentials: "include",
            headers: { "Accept": "application/json" }
        });

        if (!response.ok) {
            const errorData = await LibraryUI.readJson(response);
            throw new Error(errorData?.message || "Hủy yêu cầu thất bại.");
        }

        await loadPendingReservations();
    } catch (error) {
        alert(error.message || "Hủy yêu cầu thất bại.");
        button.disabled = false;
        button.textContent = "Hủy";
    }
}

function renderHistoryRows(records) {
    const tableBody = document.getElementById("history-table-body");
    const emptyRow = document.getElementById("history-empty-row");
    if (!tableBody) return;

    tableBody.querySelectorAll(".history-row").forEach(row => row.remove());
    records.forEach(record => tableBody.insertBefore(createHistoryRow(record), emptyRow));
}

function createHistoryRow(record) {
    const statusKey = resolveStatusKey(record);
    const row = document.createElement("tr");
    row.className = "history-row";
    row.dataset.title = record.bookTitle || "";
    row.dataset.month = record.borrowDate ? record.borrowDate.slice(0, 7) : "";
    row.dataset.status = statusKey;
    row.dataset.rawStatus = record.status || "";

    row.innerHTML = `
        <td><strong>${LibraryUI.escapeHtml(record.bookTitle || "Không có tên sách")}</strong></td>
        <td>${LibraryUI.formatDate(record.borrowDate)}</td>
        <td>${LibraryUI.formatDate(record.dueDate)}</td>
        <td>${LibraryUI.formatDate(record.returnDate)}</td>
        <td>${statusBadge(statusKey)}</td>
    `;

    return row;
}

function applyHistoryFilters() {
    const keyword = LibraryUI.normalizeText(document.getElementById("history-search")?.value || "");
    const month = document.getElementById("history-month")?.value || "";
    const status = document.getElementById("history-status")?.value || "";
    let visibleCount = 0;

    document.querySelectorAll(".history-row").forEach(row => {
        const visible = LibraryUI.normalizeText(row.dataset.title).includes(keyword)
            && (!month || row.dataset.month === month)
            && (!status || row.dataset.status === status || row.dataset.rawStatus === status);

        row.classList.toggle("d-none", !visible);
        if (visible) visibleCount += 1;
    });

    LibraryUI.setText("history-visible-count", visibleCount);
    document.getElementById("history-empty-row")?.classList.toggle("d-none", visibleCount > 0);
}

function updateHistoryStats(records) {
    const returnedCount = records.filter(record => record.status === "RETURNED").length;
    const lateCount = records.filter(record => resolveStatusKey(record) === "late" || record.status === "OVERDUE").length;

    LibraryUI.setText("history-total-returned", returnedCount);
    LibraryUI.setText("history-late-count", lateCount);
    LibraryUI.setText("history-late-fee", `${(lateCount * 5000).toLocaleString("vi-VN")}đ`);
}

function resolveStatusKey(record) {
    if (record.status === "RETURNED" && record.returnDate && record.dueDate && record.returnDate > record.dueDate) {
        return "late";
    }

    return record.status === "RETURNED" ? "on-time" : record.status || "";
}

function statusBadge(status) {
    const statusInfo = HISTORY_STATUS[status] || { className: "borrowing", label: "Không rõ" };
    return `<span class="history-status ${statusInfo.className}">${statusInfo.label}</span>`;
}

function emptyMarkup(icon, text) {
    return `
        <div class="history-empty">
            <i class="bi ${icon}"></i>
            <span>${text}</span>
        </div>
    `;
}

async function fetchJson(url) {
    const response = await fetch(url, {
        credentials: "include",
        headers: { "Accept": "application/json" }
    });

    if (!response.ok) {
        throw new Error(`Request failed: ${url}`);
    }

    return response.json();
}
