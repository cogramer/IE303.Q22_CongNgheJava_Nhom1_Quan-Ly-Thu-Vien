document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("reservation-search");
    const statusFilter = document.getElementById("reservation-status-filter");
    const modalElement = document.getElementById("reservationDetailModal");
    const detailModal = modalElement ? new bootstrap.Modal(modalElement) : null;

    searchInput?.addEventListener("input", filterReservations);
    statusFilter?.addEventListener("change", filterReservations);

    document.querySelectorAll(".avatar[data-name]").forEach(avatar => {
        avatar.textContent = LibraryUI.getInitials(avatar.dataset.name);
    });

    document.querySelectorAll(".view-reservation-btn").forEach(button => {
        button.addEventListener("click", () => {
            const row = button.closest(".reservation-row");
            if (!row || !detailModal) {
                return;
            }

            LibraryUI.setText("detail-reader", row.dataset.reader || "Chưa rõ");
            LibraryUI.setText("detail-username", row.dataset.username || "Chưa rõ");
            LibraryUI.setText("detail-book", row.dataset.book || "Chưa rõ");
            LibraryUI.setText("detail-date", row.dataset.date || "Chưa rõ");
            LibraryUI.setText("detail-copies", `${row.dataset.copies || 0} còn`);
            LibraryUI.setText("detail-status", translateStatus(row.dataset.status));

            detailModal.show();
        });
    });

    filterReservations();
});

function filterReservations() {
    const keyword = LibraryUI.normalizeText(document.getElementById("reservation-search")?.value || "");
    const status = document.getElementById("reservation-status-filter")?.value || "";
    let visibleCount = 0;

    document.querySelectorAll(".reservation-row").forEach(row => {
        const haystack = LibraryUI.normalizeText(`${row.dataset.reader || ""} ${row.dataset.username || ""} ${row.dataset.book || ""}`);
        const statusMatched = !status || row.dataset.status === status;
        const keywordMatched = !keyword || haystack.includes(keyword);
        const visible = statusMatched && keywordMatched;

        row.classList.toggle("d-none", !visible);
        if (visible) {
            visibleCount += 1;
        }
    });

    const visibleElement = document.getElementById("reservation-visible-count");
    if (visibleElement) {
        visibleElement.textContent = visibleCount;
    }
}

function translateStatus(status) {
    switch (status) {
        case "PENDING":
            return "Chờ duyệt";
        case "FULFILLED":
            return "Đã duyệt";
        case "CANCELLED":
            return "Đã hủy";
        case "NOTIFIED":
            return "Đã thông báo";
        default:
            return status || "Chưa rõ";
    }
}
