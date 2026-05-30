document.addEventListener("DOMContentLoaded", () => {
    const modalElement = document.getElementById("returnConfirmModal");
    const returnModal = new bootstrap.Modal(modalElement);
    const renewModalElement = document.getElementById("renewConfirmModal");
    const renewModal = renewModalElement ? new bootstrap.Modal(renewModalElement) : null;
    const selectAll = document.getElementById("select-all-loans");
    const returnSelectedButton = document.getElementById("return-selected-btn");
    const searchInput = document.getElementById("loan-search");
    const statusFilter = document.getElementById("loan-status-filter");
    const dateType = document.getElementById("loan-date-type");
    const dateFilter = document.getElementById("loan-date-filter");

    document.querySelectorAll(".loan-checkbox").forEach(checkbox => {
        checkbox.addEventListener("change", updateSelectionState);
    });

    if (selectAll) {
        selectAll.addEventListener("change", () => {
            document.querySelectorAll(".loan-row:not(.d-none) .loan-checkbox:not(:disabled)").forEach(checkbox => {
                checkbox.checked = selectAll.checked;
            });
            updateSelectionState();
        });
    }

    [searchInput, statusFilter, dateType, dateFilter].forEach(element => {
        if (element) {
            element.addEventListener("input", applyLoanFilters);
            element.addEventListener("change", applyLoanFilters);
        }
    });

    document.querySelectorAll(".single-return-btn").forEach(button => {
        button.addEventListener("click", () => {
            const row = button.closest("tr");
            openReturnModal([row], returnModal);
        });
    });

    document.querySelectorAll(".renew-loan-btn").forEach(button => {
        button.addEventListener("click", () => {
            const row = button.closest("tr");
            openRenewModal(row, renewModal);
        });
    });

    if (returnSelectedButton) {
        returnSelectedButton.addEventListener("click", () => {
            const selectedRows = getSelectedRows();
            openReturnModal(selectedRows, returnModal);
        });
    }

    document.getElementById("return-confirm-form").addEventListener("submit", returnSelectedLoans);
    document.getElementById("renew-confirm-form")?.addEventListener("submit", renewLoan);

    if (searchInput) {
        searchInput.focus();
        const valueLength = searchInput.value.length;
        searchInput.setSelectionRange(valueLength, valueLength);
    }

    applyLoanFilters();
    updateSelectionState();
});

function openRenewModal(row, modal) {
    if (!row || !modal) {
        return;
    }

    const form = document.getElementById("renew-confirm-form");
    const daysInput = document.getElementById("renew-days");
    const errorMessage = document.getElementById("renew-error-message");

    form.action = `/librarian/loans/renew/${encodeURIComponent(row.dataset.loanId || "")}`;
    daysInput.value = "7";
    errorMessage.textContent = "";

    LibraryUI.setText("renew-reader-name", row.dataset.reader || "");
    LibraryUI.setText("renew-book-title", row.dataset.book || "");
    LibraryUI.setText("renew-current-due-date", row.dataset.dueDate || "");

    modal.show();
}

async function renewLoan(event) {
    event.preventDefault();

    const form = event.currentTarget;
    const daysInput = document.getElementById("renew-days");
    const errorMessage = document.getElementById("renew-error-message");
    const button = document.getElementById("confirm-renew-btn");
    const days = Number(daysInput.value);

    if (!Number.isInteger(days) || days < 1 || days > 30) {
        errorMessage.textContent = "Số ngày gia hạn phải từ 1 đến 30 ngày.";
        daysInput.focus();
        return;
    }

    const body = new URLSearchParams();
    body.set("days", String(days));
    const csrfInput = form.querySelector("input[name='_csrf']");
    if (csrfInput) {
        body.set(csrfInput.name, csrfInput.value);
    }

    button.disabled = true;
    button.textContent = "Đang gia hạn...";
    errorMessage.textContent = "";

    try {
        const response = await fetch(form.action, {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body
        });

        if (!response.ok) {
            throw new Error("Renew request failed");
        }

        window.location.reload();
    } catch (error) {
        button.disabled = false;
        button.textContent = "Xác nhận gia hạn";
        errorMessage.textContent = "Gia hạn thất bại. Vui lòng kiểm tra lại số ngày.";
    }
}

function updateSelectionState() {
    const checkboxes = Array.from(document.querySelectorAll(".loan-row:not(.d-none) .loan-checkbox:not(:disabled)"));
    const checked = checkboxes.filter(checkbox => checkbox.checked);
    const selectAll = document.getElementById("select-all-loans");

    document.getElementById("selected-loan-count").textContent = checked.length;
    document.getElementById("return-selected-btn").disabled = checked.length === 0;

    if (selectAll) {
        selectAll.checked = checkboxes.length > 0 && checked.length === checkboxes.length;
        selectAll.indeterminate = checked.length > 0 && checked.length < checkboxes.length;
    }
}

function getSelectedRows() {
    return Array.from(document.querySelectorAll(".loan-checkbox:checked"))
        .map(checkbox => checkbox.closest("tr"))
        .filter(row => !row.classList.contains("d-none"));
}

function applyLoanFilters() {
    const keyword = LibraryUI.normalizeText(document.getElementById("loan-search").value);
    const status = document.getElementById("loan-status-filter").value;
    const dateType = document.getElementById("loan-date-type").value;
    const dateValue = document.getElementById("loan-date-filter").value;
    let visibleCount = 0;

    document.querySelectorAll(".loan-row").forEach(row => {
        const searchable = LibraryUI.normalizeText(`${row.dataset.reader} ${row.dataset.book}`);
        const rowDate = dateType === "due" ? row.dataset.dueDateRaw : row.dataset.borrowDate;
        const visible =
            searchable.includes(keyword) &&
            (!status || row.dataset.status === status) &&
            (!dateValue || rowDate === dateValue);

        row.classList.toggle("d-none", !visible);

        if (visible) {
            visibleCount += 1;
        }

        if (!visible) {
            const checkbox = row.querySelector(".loan-checkbox");
            if (checkbox) checkbox.checked = false;
        }
    });

    const totalCount = document.getElementById("loan-total-count");
    if (totalCount) {
        totalCount.textContent = visibleCount;
    }

    updateSelectionState();
}

function openReturnModal(rows, modal) {
    if (!rows.length) {
        return;
    }

    const tbody = document.getElementById("return-confirm-body");
    const fields = document.getElementById("return-loan-id-fields");

    tbody.innerHTML = rows.map(row => `
        <tr>
            <td>${LibraryUI.escapeHtml(row.dataset.reader || "")}</td>
            <td><strong>${LibraryUI.escapeHtml(row.dataset.book || "")}</strong></td>
            <td>${LibraryUI.escapeHtml(row.dataset.dueDate || "")}</td>
        </tr>
    `).join("");

    fields.innerHTML = rows.map(row => `
        <input type="hidden" name="loanIds" value="${LibraryUI.escapeHtml(row.dataset.loanId || "")}">
    `).join("");

    const button = document.getElementById("confirm-return-btn");
    button.disabled = false;
    button.textContent = rows.length > 1 ? `Xác nhận trả ${rows.length} sách` : "Xác nhận trả";

    modal.show();
}

function returnSelectedLoans(event) {
    const loanIds = document.querySelectorAll("#return-loan-id-fields input[name='loanIds']");

    // Chặn submit form nếu không có phiếu mượn nào được chọn
    if (!loanIds || loanIds.length === 0) {
        event.preventDefault();
        return;
    }

    const button = document.getElementById("confirm-return-btn");

    // Khóa nút bấm để tránh người dùng spam click (double submit)
    if (button.dataset.submitting === "true") {
        event.preventDefault();
        return;
    }

    button.dataset.submitting = "true";
    button.textContent = "Đang xử lý...";

    // ĐIỂM MẤU CHỐT: Không gọi event.preventDefault() ở nhánh thành công.
    // Hãy để trình duyệt tự động POST data lên server. 
    // Hệ thống sẽ tự redirect và tải lại giao diện hoàn chỉnh.
}