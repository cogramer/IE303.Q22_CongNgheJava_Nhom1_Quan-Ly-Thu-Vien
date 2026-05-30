/* ===== HISTORY PAGE JAVASCRIPT ===== */

document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('searchInput');
    const dateRangeInput = document.getElementById('dateRangeInput');
    const statusFilter = document.getElementById('statusFilter');
    const historyTableBody = document.getElementById('historyTableBody');
    const totalRecordCount = document.getElementById('totalRecordCount');

    // Lưu trữ dữ liệu gốc từ bảng
    let allRecords = [];

    // Khởi tạo
    initializeTable();
    attachEventListeners();

    /**
     * Khởi tạo bảng - lấy tất cả rows hiện tại
     */
    function initializeTable() {
        const rows = historyTableBody.querySelectorAll('tr');
        allRecords = [];

        rows.forEach((row, index) => {
            const sttCell = row.querySelector('.col-stt');
            const bookTitleCell = row.querySelector('.book-title');
            const bookAuthorCell = row.querySelector('.book-author');
            const borrowDateCell = row.querySelector('.col-borrow-date');
            const returnDateCell = row.querySelector('.col-return-date');
            const statusCell = row.querySelector('.status-badge');
            const feeCell = row.querySelector('.fee-amount');

            // Bỏ qua empty state
            if (!sttCell || sttCell.textContent.trim() === '' || row.classList.contains('empty-state')) {
                return;
            }

            allRecords.push({
                index: index,
                row: row.cloneNode(true),
                bookTitle: bookTitleCell ? bookTitleCell.textContent.toLowerCase() : '',
                bookAuthor: bookAuthorCell ? bookAuthorCell.textContent.toLowerCase() : '',
                borrowDate: borrowDateCell ? borrowDateCell.textContent.trim() : '',
                returnDate: returnDateCell ? returnDateCell.textContent.trim() : '',
                status: statusCell ? statusCell.textContent.trim() : '',
                fee: feeCell ? feeCell.textContent.trim() : '-',
            });
        });

        updateTotalCount();
    }

    /**
     * Gắn event listeners
     */
    function attachEventListeners() {
        searchInput.addEventListener('input', debounce(applyFilters, 300));
        dateRangeInput.addEventListener('change', applyFilters);
        statusFilter.addEventListener('change', applyFilters);
    }

    /**
     * Áp dụng tất cả filters
     */
    function applyFilters() {
        const searchTerm = searchInput.value.toLowerCase().trim();
        const selectedDate = dateRangeInput.value; // Format: YYYY-MM
        const selectedStatus = statusFilter.value;

        // Lọc records
        const filteredRecords = allRecords.filter(record => {
            // Lọc theo tìm kiếm
            if (searchTerm) {
                const matchesSearch =
                    record.bookTitle.includes(searchTerm) ||
                    record.bookAuthor.includes(searchTerm);
                if (!matchesSearch) return false;
            }

            // Lọc theo ngày
            if (selectedDate) {
                const recordMonth = extractMonth(record.borrowDate); // Format: MM/YYYY
                const selectedMonth = formatDate(selectedDate); // Convert YYYY-MM to MM/YYYY
                if (recordMonth !== selectedMonth) return false;
            }

            // Lọc theo trạng thái
            if (selectedStatus) {
                const recordStatus = record.status.toLowerCase();
                const filterStatus = selectedStatus.toLowerCase();
                
                if (filterStatus === 'on-time' && recordStatus !== 'đã trả') return false;
                if (filterStatus === 'late' && recordStatus !== 'trễ hạn') return false;
            }

            return true;
        });

        // Hiển thị kết quả lọc
        displayFilteredRecords(filteredRecords);
        updateTotalCount(filteredRecords.length);
    }

    /**
     * Hiển thị records đã lọc
     */
    function displayFilteredRecords(records) {
        historyTableBody.innerHTML = '';

        if (records.length === 0) {
            const emptyRow = document.createElement('tr');
            emptyRow.classList.add('empty-state');
            emptyRow.innerHTML = `
                <td colspan="6" class="text-center py-5">
                    <div class="empty-state-content">
                        <i class="bi bi-inbox"></i>
                        <p class="mb-0">Không tìm thấy kết quả phù hợp</p>
                    </div>
                </td>
            `;
            historyTableBody.appendChild(emptyRow);
            return;
        }

        records.forEach((record, index) => {
            const newRow = record.row.cloneNode(true);
            // Cập nhật STT
            const sttCell = newRow.querySelector('.col-stt');
            if (sttCell) {
                sttCell.textContent = index + 1;
            }
            historyTableBody.appendChild(newRow);
        });
    }

    /**
     * Cập nhật tổng số bản ghi
     */
    function updateTotalCount(count = allRecords.length) {
        totalRecordCount.textContent = count;
    }

    /**
     * Extract tháng từ chuỗi ngày (DD/MM/YYYY) -> MM/YYYY
     */
    function extractMonth(dateString) {
        const parts = dateString.split('/');
        if (parts.length === 3) {
            return `${parts[1]}/${parts[2]}`; // MM/YYYY
        }
        return '';
    }

    /**
     * Convert từ YYYY-MM sang MM/YYYY
     */
    function formatDate(dateString) {
        const [year, month] = dateString.split('-');
        return `${month}/${year}`;
    }

    /**
     * Debounce function - tránh gọi hàm quá nhiều lần
     */
    function debounce(func, delay) {
        let timeout;
        return function (...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), delay);
        };
    }

    // ===== PAGINATION HANDLING (Optional - nếu cần) =====
    // Có thể thêm pagination logic tại đây nếu dữ liệu quá lớn
    handlePagination();

    function handlePagination() {
        // Tạm thời disable pagination buttons vì dữ liệu từ server đã được chia sẵn
        const paginationItems = document.querySelectorAll('.page-item');
        paginationItems.forEach(item => {
            const link = item.querySelector('.page-link');
            if (link) {
                link.addEventListener('click', (e) => {
                    e.preventDefault();
                    // Logic phân trang sẽ được thêm khi cần
                });
            }
        });
    }

    // ===== SORT COLUMN FUNCTIONALITY (Optional) =====
    attachColumnSortListeners();

    function attachColumnSortListeners() {
        const headers = document.querySelectorAll('.table-header-custom th');
        headers.forEach(header => {
            header.style.cursor = 'pointer';
            header.addEventListener('click', function () {
                // Có thể thêm sắp xếp theo cột
                console.log('Sort by:', this.textContent);
            });
        });
    }

    // ===== EXPORT FUNCTIONALITY (Optional) =====
    // Có thể thêm nút Export PDF/Excel trong tương lai

    /**
     * Clear all filters
     */
    window.clearAllFilters = function () {
        searchInput.value = '';
        dateRangeInput.value = '';
        statusFilter.value = '';
        displayFilteredRecords(allRecords);
        updateTotalCount();
    };

    // Thêm keyboard shortcuts
    document.addEventListener('keydown', function (e) {
        // Ctrl+F hoặc Cmd+F để focus vào search
        if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
            e.preventDefault();
            searchInput.focus();
        }

        // Escape để clear search
        if (e.key === 'Escape' && document.activeElement === searchInput) {
            searchInput.value = '';
            applyFilters();
        }
    });
});
