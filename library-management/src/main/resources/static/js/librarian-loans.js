document.addEventListener("DOMContentLoaded", () => {
    const modalElement = document.getElementById("returnConfirmModal");
    const returnModal = new bootstrap.Modal(modalElement);
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

    if (returnSelectedButton) {
        returnSelectedButton.addEventListener("click", () => {
            const selectedRows = getSelectedRows();
            openReturnModal(selectedRows, returnModal);
        });
    }

    document.getElementById("return-confirm-form").addEventListener("submit", returnSelectedLoans);

    if (searchInput) {
        searchInput.focus();
        const valueLength = searchInput.value.length;
        searchInput.setSelectionRange(valueLength, valueLength);
    }

    applyLoanFilters();
    updateSelectionState();
});

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

async function returnSelectedLoans(event) {
    event.preventDefault();

    const button = document.getElementById("confirm-return-btn");
    const loanIds = Array.from(document.querySelectorAll("#return-loan-id-fields input[name='loanIds']"))
        .map(input => input.value)
        .filter(Boolean);

    if (!loanIds.length) {
        return;
    }

    button.disabled = true;
    button.textContent = "Đang trả...";

    try {
        for (const loanId of loanIds) {
            const response = await fetch(`/librarian/loans/return/${encodeURIComponent(loanId)}`, {
                method: "POST",
                credentials: "include"
            });

            if (!response.ok) {
                throw new Error("Return request failed");
            }
        }

        window.location.reload();
    } catch (error) {
        button.disabled = false;
        button.textContent = loanIds.length > 1 ? `Xác nhận trả ${loanIds.length} sách` : "Xác nhận trả";
        alert("Trả sách thất bại. Vui lòng thử lại.");
    }
}
