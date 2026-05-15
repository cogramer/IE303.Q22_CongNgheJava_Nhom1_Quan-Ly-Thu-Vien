document.addEventListener("DOMContentLoaded", () => {
    renderBorrowMonthChart();
    renderDaysLate();
    bindReportFilters();
    applyReportFilters();
    bindExportButtons();
});

function renderBorrowMonthChart() {
    const canvas = document.getElementById("borrowMonthChart");
    const data = document.getElementById("reports-data");

    if (!canvas || !data || typeof Chart === "undefined") {
        return;
    }

    const monthLabels = (data.dataset.borrowMonths || "")
        .split(",")
        .filter(Boolean)
        .map(month => `Tháng ${month}`);
    const monthValues = (data.dataset.borrowValues || "")
        .split(",")
        .filter(Boolean)
        .map(value => Number(value) || 0);

    new Chart(canvas, {
        type: "line",
        data: {
            labels: monthLabels,
            datasets: [{
                label: "Lượt mượn",
                data: monthValues,
                borderColor: "#0d6efd",
                backgroundColor: "rgba(13, 110, 253, 0.12)",
                borderWidth: 3,
                fill: true,
                tension: 0.35,
                pointRadius: 4,
                pointBackgroundColor: "#0d6efd"
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    },
                    grid: {
                        color: "rgba(13, 71, 161, 0.08)"
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

function bindReportFilters() {
    ["report-from-date", "report-to-date", "report-status-filter"].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener("input", applyReportFilters);
            element.addEventListener("change", applyReportFilters);
        }
    });

    const resetButton = document.getElementById("reset-report-filter");
    if (resetButton) {
        resetButton.addEventListener("click", () => {
            document.getElementById("report-from-date").value = "";
            document.getElementById("report-to-date").value = "";
            document.getElementById("report-status-filter").value = "";
            applyReportFilters();
        });
    }
}

function applyReportFilters() {
    const fromDate = document.getElementById("report-from-date")?.value || "";
    const toDate = document.getElementById("report-to-date")?.value || "";
    const status = document.getElementById("report-status-filter")?.value || "";
    const rows = Array.from(document.querySelectorAll(".report-loan-row"));
    const counts = {
        total: 0,
        borrowing: 0,
        returned: 0,
        overdue: 0
    };

    rows.forEach(row => {
        const borrowDate = row.dataset.borrowDate || "";
        const rowStatus = row.dataset.status || "";
        const visible =
            (!fromDate || borrowDate >= fromDate) &&
            (!toDate || borrowDate <= toDate) &&
            (!status || rowStatus === status);

        row.classList.toggle("d-none", !visible);

        if (visible) {
            counts.total += 1;
            if (rowStatus === "BORROWING") counts.borrowing += 1;
            if (rowStatus === "RETURNED") counts.returned += 1;
            if (rowStatus === "OVERDUE") counts.overdue += 1;
        }
    });

    LibraryUI.setText("filtered-total-loans", counts.total);
    LibraryUI.setText("filtered-overdue-loans", counts.overdue);
    LibraryUI.setText("visible-loan-count", counts.total);
    LibraryUI.setText("status-borrowing-count", counts.borrowing);
    LibraryUI.setText("status-returned-count", counts.returned);
    LibraryUI.setText("status-overdue-count", counts.overdue);
    renderTopReaders();
}

function renderTopReaders() {
    const body = document.getElementById("top-readers-body");

    if (!body) {
        return;
    }

    const counts = new Map();
    document.querySelectorAll(".report-loan-row:not(.d-none)").forEach(row => {
        const user = row.dataset.user || "Không rõ";
        counts.set(user, (counts.get(user) || 0) + 1);
    });

    const readers = Array.from(counts.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5);

    if (!readers.length) {
        body.innerHTML = `
            <tr>
                <td colspan="2" class="empty-text">Chưa có dữ liệu độc giả.</td>
            </tr>
        `;
        return;
    }

    body.innerHTML = readers.map(([user, count]) => `
        <tr>
            <td><strong>${LibraryUI.escapeHtml(user)}</strong></td>
            <td class="text-end"><span class="count-pill">${count}</span></td>
        </tr>
    `).join("");
}

function renderDaysLate() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    document.querySelectorAll(".overdue-table tbody tr[data-due-date]").forEach(row => {
        const dueDate = new Date(row.dataset.dueDate);
        const target = row.querySelector(".days-late");

        if (!target || Number.isNaN(dueDate.getTime())) {
            return;
        }

        dueDate.setHours(0, 0, 0, 0);
        const diff = Math.max(0, Math.ceil((today - dueDate) / 86400000));
        target.textContent = diff;
    });
}

function bindExportButtons() {
    const pdfButton = document.getElementById("export-pdf-btn");
    const excelButton = document.getElementById("export-excel-btn");

    if (pdfButton) {
        pdfButton.addEventListener("click", () => window.print());
    }

    if (excelButton) {
        excelButton.addEventListener("click", exportReportCsv);
    }
}

function exportReportCsv() {
    const rows = [["Section", "Name", "Value"]];

    document.querySelectorAll(".stat-card").forEach(card => {
        const label = card.querySelector("span")?.textContent?.trim() || "";
        const value = card.querySelector("strong")?.textContent?.trim() || "";
        rows.push(["Summary", label, value]);
    });

    document.querySelectorAll(".report-table tbody tr:not(.d-none)").forEach(row => {
        const cells = Array.from(row.querySelectorAll("td")).map(cell => cell.textContent.trim().replace(/\s+/g, " "));
        if (cells.length > 1) {
            rows.push(["Table", cells[0], cells.slice(1).join(" | ")]);
        }
    });

    const csv = rows.map(row => row.map(escapeCsv).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = "library-reports.csv";
    link.click();
    URL.revokeObjectURL(url);
}

function escapeCsv(value) {
    return `"${String(value ?? "").replaceAll('"', '""')}"`;
}
