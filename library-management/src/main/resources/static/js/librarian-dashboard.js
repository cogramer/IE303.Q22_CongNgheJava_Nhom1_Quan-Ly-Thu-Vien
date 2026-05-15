document.addEventListener("DOMContentLoaded", () => {
    initDashboardAvatars();

    const data = document.getElementById("dashboard-data");

    if (!data || typeof Chart === "undefined") {
        return;
    }

    renderBorrowMonthChart(data);
    renderBorrowingRateChart(data);
});

function initDashboardAvatars() {
    document.querySelectorAll(".dashboard-avatar[data-name]").forEach(avatar => {
        avatar.textContent = window.LibraryUI
            ? LibraryUI.getInitials(avatar.dataset.name)
            : getFallbackInitials(avatar.dataset.name);
    });
}

function getFallbackInitials(value) {
    const words = String(value || "U").trim().split(/\s+/).filter(Boolean);
    if (!words.length) return "U";
    if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
    return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase();
}

function renderBorrowMonthChart(data) {
    const canvas = document.getElementById("borrowMonthChart");
    if (!canvas) return;

    const labels = splitData(data.dataset.borrowMonths).map(month => `Tháng ${month}`);
    const values = splitData(data.dataset.borrowValues).map(value => Number(value || 0));

    new Chart(canvas, {
        type: "line",
        data: {
            labels,
            datasets: [{
                label: "Lượt mượn",
                data: values,
                borderColor: "#0d6efd",
                backgroundColor: "rgba(13, 110, 253, 0.12)",
                fill: true,
                tension: 0.35,
                pointRadius: 4,
                pointBackgroundColor: "#0d6efd"
            }]
        },
        options: baseChartOptions(false)
    });
}

function renderBorrowingRateChart(data) {
    const canvas = document.getElementById("borrowingRateChart");
    if (!canvas) return;

    const borrowed = Number(data.dataset.borrowedCopies || 0);
    const available = Number(data.dataset.availableCopies || 0);

    new Chart(canvas, {
        type: "doughnut",
        data: {
            labels: ["Đang mượn", "Còn sách"],
            datasets: [{
                data: [borrowed, available],
                backgroundColor: ["#0d6efd", "#d9e8ff"],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            cutout: "68%",
            plugins: {
                legend: {
                    position: "bottom"
                }
            }
        }
    });
}

function baseChartOptions(showLegend) {
    return {
        responsive: true,
        plugins: {
            legend: {
                display: showLegend
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                ticks: {
                    precision: 0
                },
                grid: {
                    color: "rgba(15, 23, 42, 0.08)"
                }
            },
            x: {
                grid: {
                    display: false
                }
            }
        }
    };
}

function splitData(value) {
    if (!value) {
        return [];
    }
    return value.split(",").map(item => item.trim());
}
