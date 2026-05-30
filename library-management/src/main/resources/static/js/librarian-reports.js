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
    document.querySelectorAll(".overdue-table tbody tr[data-due-date]").forEach(row => {
        const target = row.querySelector(".days-late");

        if (!target) {
            return;
        }

        target.textContent = getDaysLate(row.dataset.dueDate);
    });
}

function bindExportButtons() {
    document.getElementById("export-pdf-btn")?.addEventListener("click", exportReportPdf);
    document.getElementById("export-excel-btn")?.addEventListener("click", exportReportExcel);
}

function exportReportPdf() {
    const originalTitle = document.title;
    document.title = `bao-cao-thu-vien-${getReportTimestamp()}`;
    showExportStatus("Đang mở hộp thoại in PDF...");

    setTimeout(() => {
        window.print();
        document.title = originalTitle;
    }, 100);
}

function exportReportExcel() {
    const sheets = [];
    const summaryRows = [
        ["Thời điểm xuất", new Date().toLocaleString("vi-VN")],
        ["Bộ lọc", getFilterSummary()],
        [],
        ["Chỉ số", "Giá trị"]
    ];

    document.querySelectorAll(".stat-card").forEach(card => {
        const label = card.querySelector("span")?.textContent?.trim() || "";
        const value = card.querySelector("strong")?.textContent?.trim() || "";
        summaryRows.push([label, value]);
    });

    sheets.push(createSheet("Tổng quan", summaryRows));

    const reportGrids = document.querySelectorAll(".reports-grid");
    sheets.push(createTableSheet("Top sách", reportGrids[0]?.querySelector("article:nth-child(1) .report-table")));
    sheets.push(createTableSheet("Top độc giả", reportGrids[0]?.querySelector("article:nth-child(2) .report-table")));
    sheets.push(createTableSheet("Thể loại", reportGrids[1]?.querySelector("article:nth-child(1) .report-table")));
    sheets.push(createTableSheet("Lượt mượn", ".loan-report-table"));
    const workbook = `<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
          xmlns:o="urn:schemas-microsoft-com:office:office"
          xmlns:x="urn:schemas-microsoft-com:office:excel"
          xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
    ${sheets.filter(Boolean).join("\n")}
</Workbook>`;

    const blob = new Blob(["\uFEFF" + workbook], { type: "application/vnd.ms-excel;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = `bao-cao-thu-vien-${getReportTimestamp()}.xls`;
    link.click();
    URL.revokeObjectURL(url);
    showExportStatus("Đã xuất file Excel thành công.");
}

function createTableSheet(name, tableOrSelector) {
    const table = typeof tableOrSelector === "string"
        ? document.querySelector(tableOrSelector)
        : tableOrSelector;

    if (!table) {
        return createSheet(name, [["Không có dữ liệu"]]);
    }

    const headers = Array.from(table.querySelectorAll("thead th")).map(cleanCellText);
    const bodyRows = Array.from(table.querySelectorAll("tbody tr:not(.d-none)"))
        .map(row => Array.from(row.querySelectorAll("td")).map(cleanCellText))
        .filter(cells => cells.length > 0 && !cells.every(cell => !cell));

    const rows = [];
    if (headers.length) rows.push(headers);
    rows.push(...bodyRows);

    if (!bodyRows.length) {
        rows.push(["Không có dữ liệu"]);
    }

    return createSheet(name, rows);
}

function getDaysLate(dueDateValue) {
    const dueDate = new Date(dueDateValue);
    if (Number.isNaN(dueDate.getTime())) {
        return 0;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    dueDate.setHours(0, 0, 0, 0);

    return Math.max(0, Math.ceil((today - dueDate) / 86400000));
}

function createSheet(name, rows) {
    const safeName = escapeXml(name).slice(0, 31);
    const tableRows = rows.map(row => `
        <Row>
            ${row.map(cell => `<Cell><Data ss:Type="String">${escapeXml(cell)}</Data></Cell>`).join("")}
        </Row>
    `).join("");

    return `
        <Worksheet ss:Name="${safeName}">
            <Table>${tableRows}</Table>
        </Worksheet>
    `;
}

function cleanCellText(cell) {
    return (cell?.textContent || "").trim().replace(/\s+/g, " ");
}

function getFilterSummary() {
    const fromDate = document.getElementById("report-from-date")?.value || "Không chọn";
    const toDate = document.getElementById("report-to-date")?.value || "Không chọn";
    const statusSelect = document.getElementById("report-status-filter");
    const status = statusSelect?.selectedOptions?.[0]?.textContent?.trim() || "Tất cả";
    return `Từ ngày: ${fromDate}; Đến ngày: ${toDate}; Trạng thái: ${status}`;
}

function getReportTimestamp() {
    const now = new Date();
    const pad = value => String(value).padStart(2, "0");
    return [
        now.getFullYear(),
        pad(now.getMonth() + 1),
        pad(now.getDate())
    ].join("-") + "-" + [pad(now.getHours()), pad(now.getMinutes())].join("-");
}

function showExportStatus(message) {
    const element = document.getElementById("export-status");
    if (!element) {
        return;
    }

    element.textContent = message;
    element.classList.remove("d-none");

    window.clearTimeout(showExportStatus.timeoutId);
    showExportStatus.timeoutId = window.setTimeout(() => {
        element.classList.add("d-none");
    }, 2600);
}

function escapeXml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&apos;");
}
