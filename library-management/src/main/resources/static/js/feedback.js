const feedbackState = {
    books: [],
    feedbacks: [],
    currentPage: 0,
    pageSize: 6,
    totalPages: 1,
    totalElements: 0,
    editModal: null,
    deleteModal: null,
    editingOriginal: null
};

document.addEventListener("DOMContentLoaded", async () => {
    feedbackState.editModal = new bootstrap.Modal(document.getElementById("feedbackEditModal"));
    feedbackState.deleteModal = new bootstrap.Modal(document.getElementById("feedbackDeleteModal"));
    bindFeedbackForm();
    bindEditForm();
    bindDeleteConfirm();
    bindFeedbackPagination();
    await Promise.all([loadBooks(), loadMyFeedbacks()]);
});

function bindFeedbackForm() {
    document.getElementById("feedback-form").addEventListener("submit", async event => {
        event.preventDefault();
        await submitFeedback();
    });
}

function bindEditForm() {
    document.getElementById("feedback-edit-form").addEventListener("submit", async event => {
        event.preventDefault();
        await submitEditFeedback();
    });
}

function bindDeleteConfirm() {
    document.getElementById("feedback-delete-confirm").addEventListener("click", deleteSelectedFeedback);
}

function bindFeedbackPagination() {
    document.getElementById("feedback-prev-page")?.addEventListener("click", () => {
        if (feedbackState.currentPage > 0) {
            loadMyFeedbacks(feedbackState.currentPage - 1);
        }
    });

    document.getElementById("feedback-next-page")?.addEventListener("click", () => {
        if (feedbackState.currentPage < feedbackState.totalPages - 1) {
            loadMyFeedbacks(feedbackState.currentPage + 1);
        }
    });
}

async function loadBooks() {
    const select = document.getElementById("feedback-book");

    try {
        const response = await fetch("/api/books", { credentials: "include" });
        const books = await LibraryUI.readJson(response);

        if (!response.ok || !Array.isArray(books)) {
            throw new Error("Không tải được danh sách sách.");
        }

        feedbackState.books = books;
        renderBookOptions(books);
    } catch (error) {
        select.innerHTML = '<option value="">Không tải được danh sách sách</option>';
        showFormMessage(error.message, false);
    }
}

async function loadMyFeedbacks(page = feedbackState.currentPage) {
    try {
        const response = await fetch(
            `/api/feedbacks/me?page=${page}&size=${feedbackState.pageSize}&sortDir=desc`,
            { credentials: "include" }
        );
        const data = await LibraryUI.readJson(response);

        if (!response.ok || !Array.isArray(data.content)) {
            throw new Error(data?.message || "Không tải được đánh giá.");
        }

        feedbackState.feedbacks = data.content;
        feedbackState.currentPage = data.number || 0;
        feedbackState.totalPages = data.totalPages || 1;
        feedbackState.totalElements = data.totalElements || 0;

        renderFeedbackList(data.content);
        renderFeedbackPagination();
    } catch (error) {
        feedbackState.feedbacks = [];
        feedbackState.currentPage = 0;
        feedbackState.totalPages = 1;
        feedbackState.totalElements = 0;
        renderFeedbackList([]);
        renderFeedbackPagination();
        showFormMessage(error.message, false);
    }
}

function renderBookOptions(books) {
    const select = document.getElementById("feedback-book");
    const options = books
        .map(book => {
            const title = LibraryUI.escapeHtml(book.title || "Sách không tên");
            const author = LibraryUI.escapeHtml(book.author || "Chưa rõ tác giả");
            return `<option value="${book.id}">${title} - ${author}</option>`;
        })
        .join("");

    select.innerHTML = `<option value="">Chọn sách cần đánh giá</option>${options}`;
}

function renderFeedbackList(feedbacks) {
    const tableBody = document.getElementById("feedbackTableBody");
    const totalCount = document.getElementById("totalFeedbackCount");

    totalCount.textContent = feedbackState.totalElements;

    if (!feedbacks.length) {
        tableBody.innerHTML = `
            <tr class="empty-state">
                <td colspan="6" class="text-center py-5">
                    <div class="empty-state-content">
                        <i class="bi bi-inbox"></i>
                        <p class="mb-0">Bạn chưa gửi đánh giá nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tableBody.innerHTML = feedbacks.map((item, index) => {
        const title = LibraryUI.escapeHtml(item.bookTitle || "Sách không tên");
        const date = LibraryUI.formatDate(item.eventDate);
        const score = Number(item.score || 0);
        const comment = item.comment || "";
        const escapedComment = LibraryUI.escapeHtml(comment);
        const commentHtml = escapedComment
            ? `<div class="feedback-comment">${escapedComment}</div>`
            : '<div class="feedback-comment is-empty">Chưa có bình luận</div>';
        const order = feedbackState.currentPage * feedbackState.pageSize + index + 1;

        return `
            <tr data-feedback-id="${item.id}">
                <td class="col-stt">
                    <span>${order}</span>
                </td>
                <td class="col-book">
                    <span class="book-title">${title}</span>
                </td>
                <td class="col-score">
                    <span class="feedback-score">${renderStars(score)} ${score || "-"}</span>
                </td>
                <td class="col-comment">
                    <span>${commentHtml}</span>
                </td>
                <td class="col-date">
                    <span>${date}</span>
                </td>
                <td class="col-action">
                    <div class="feedback-actions">
                        <button class="btn btn-outline-primary feedback-update-btn" type="button">Sửa</button>
                        <button class="btn btn-outline-danger feedback-delete-btn" type="button">Xóa</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");

    bindFeedbackActions();
}

function renderFeedbackPagination() {
    const pagination = document.getElementById("feedback-pagination");
    const pageInfo = document.getElementById("feedback-page-info");
    const prevButton = document.getElementById("feedback-prev-page");
    const nextButton = document.getElementById("feedback-next-page");

    if (!pagination || !pageInfo || !prevButton || !nextButton) {
        return;
    }

    pagination.hidden = feedbackState.totalPages <= 1;
    pageInfo.textContent = `Trang ${feedbackState.currentPage + 1} / ${feedbackState.totalPages}`;
    prevButton.disabled = feedbackState.currentPage <= 0;
    nextButton.disabled = feedbackState.currentPage >= feedbackState.totalPages - 1;
}

function renderStars(score) {
    const filled = Math.max(1, Math.min(5, Math.round(score || 1)));
    return "&#9733;".repeat(filled) + "&#9734;".repeat(5 - filled);
}

function bindFeedbackActions() {
    document.querySelectorAll(".feedback-update-btn").forEach(button => {
        button.addEventListener("click", () => openEditModal(button.closest("tr")));
    });

    document.querySelectorAll(".feedback-delete-btn").forEach(button => {
        button.addEventListener("click", () => openDeleteModal(button.closest("tr")));
    });
}

async function submitFeedback() {
    const submitButton = document.getElementById("feedback-submit");
    const bookId = Number(document.getElementById("feedback-book").value);
    const score = Number(document.getElementById("feedback-score").value);
    const comment = document.getElementById("feedback-comment").value.trim();

    if (!bookId) {
        notifyFeedback("Vui lòng chọn sách.", false);
        return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "Đang gửi...";
    showFormMessage("", true);

    try {
        const createResponse = await fetch("/api/feedbacks", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ bookId, score, comment })
        });
        const created = await LibraryUI.readJson(createResponse);

        if (!createResponse.ok) {
            throw new Error(created?.message || "Gửi đánh giá thất bại.");
        }

        document.getElementById("feedback-form").reset();
        document.getElementById("feedback-score").value = "5";
        feedbackState.currentPage = 0;
        notifyFeedback("Gửi đánh giá thành công.", true);
        await loadMyFeedbacks(0);
    } catch (error) {
        notifyFeedback(error.message, false);
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "Gửi đánh giá";
    }
}

function findFeedbackByRow(row) {
    if (!row) {
        return null;
    }

    const id = row.dataset.feedbackId;
    return feedbackState.feedbacks.find(item => String(item.id) === String(id));
}

function openEditModal(row) {
    const feedback = findFeedbackByRow(row);

    if (!feedback) {
        notifyFeedback("Không tìm thấy feedback cần sửa.", false);
        return;
    }

    document.getElementById("edit-feedback-id").value = feedback.id;
    document.getElementById("edit-feedback-book-title").value = feedback.bookTitle || "Sách không tên";
    document.getElementById("edit-feedback-score").value = feedback.score || 5;
    document.getElementById("edit-feedback-comment").value = feedback.comment || "";
    feedbackState.editingOriginal = {
        score: Number(feedback.score || 5),
        comment: String(feedback.comment || "").trim()
    };
    showModalMessage("feedback-edit-message", "", true);
    feedbackState.editModal.show();
}

async function submitEditFeedback() {
    const id = document.getElementById("edit-feedback-id").value;
    const score = Number(document.getElementById("edit-feedback-score").value);
    const comment = document.getElementById("edit-feedback-comment").value.trim();
    const button = document.getElementById("feedback-edit-submit");
    const original = feedbackState.editingOriginal;

    if (original && original.score === score && original.comment === comment) {
        notifyFeedback("Bạn chưa thay đổi nội dung đánh giá.", false);
        return;
    }

    button.disabled = true;
    button.textContent = "Đang lưu...";

    try {
        await updateFeedbackById(id, score, comment);
        notifyFeedback("Cập nhật đánh giá thành công.", true);
        await loadMyFeedbacks(feedbackState.currentPage);
        feedbackState.editModal.hide();
    } catch (error) {
        notifyFeedback(error.message, false);
    } finally {
        button.disabled = false;
        button.textContent = "Lưu thay đổi";
    }
}

async function updateFeedbackById(id, score, comment) {
    const response = await fetch(`/api/feedbacks/${encodeURIComponent(id)}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ score, comment })
    });
    const data = await LibraryUI.readJson(response);

    if (!response.ok) {
        throw new Error(data?.message || "Cập nhật đánh giá thất bại.");
    }

    return data;
}

function openDeleteModal(row) {
    const feedback = findFeedbackByRow(row);

    if (!feedback) {
        notifyFeedback("Không tìm thấy đánh giá cần xóa.", false);
        return;
    }

    document.getElementById("delete-feedback-id").value = feedback.id;
    document.getElementById("delete-feedback-book-title").textContent = feedback.bookTitle || "Sách không tên";
    showModalMessage("feedback-delete-message", "", true);
    feedbackState.deleteModal.show();
}

async function deleteSelectedFeedback() {
    const id = document.getElementById("delete-feedback-id").value;
    const button = document.getElementById("feedback-delete-confirm");

    button.disabled = true;
    button.textContent = "Đang xóa...";

    try {
        const response = await fetch(`/api/feedbacks/${encodeURIComponent(id)}`, {
            method: "DELETE",
            credentials: "include"
        });

        if (!response.ok) {
            const data = await LibraryUI.readJson(response);
            throw new Error(data?.message || "Xóa đánh giá thất bại.");
        }

        const nextPage = feedbackState.feedbacks.length === 1 && feedbackState.currentPage > 0
            ? feedbackState.currentPage - 1
            : feedbackState.currentPage;

        feedbackState.deleteModal.hide();
        notifyFeedback("Xóa đánh giá thành công.", true);
        await loadMyFeedbacks(nextPage);
    } catch (error) {
        notifyFeedback(error.message, false);
    } finally {
        button.disabled = false;
        button.textContent = "Xóa đánh giá";
    }
}

function showFormMessage(message, success) {
}

function notifyFeedback(message, success) {
    Swal.fire({
        icon: success ? "success" : "error",
        title: success ? "Thành công" : "Lỗi",
        text: message,
        timer: 2000,
        timerProgressBar: true,
        showConfirmButton: false
    });
}

function showModalMessage(id, message, success) {
    const target = document.getElementById(id);
    if (!target) {
        return;
    }
    target.textContent = message;
    target.className = success
        ? "feedback-message is-success"
        : "feedback-message is-error";
}
