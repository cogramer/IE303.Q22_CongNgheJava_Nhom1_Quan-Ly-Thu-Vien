document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("user-search");
    const roleFilter = document.getElementById("role-filter");

    if (searchInput) {
        searchInput.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();
                submitBackendSearch();
            }
        });
    }

    if (roleFilter) {
        roleFilter.addEventListener("input", applyUserFilters);
        roleFilter.addEventListener("change", applyUserFilters);
    }

    document.querySelectorAll(".user-avatar[data-name]").forEach(avatar => {
        avatar.textContent = LibraryUI.getInitials(avatar.dataset.name);
    });

    document.querySelectorAll(".view-user-btn").forEach(button => {
        button.addEventListener("click", () => {
            openDetailModal(button.closest(".user-row"));
        });
    });

    document.querySelectorAll(".edit-user-btn").forEach(button => {
        button.addEventListener("click", () => {
            openEditModal(button.closest(".user-row"));
        });
    });

    document.querySelectorAll(".delete-user-btn").forEach(button => {
        button.addEventListener("click", () => {
            openDeleteModal(button.closest(".user-row"));
        });
    });

    const editForm = document.getElementById("edit-user-form");
    if (editForm) {
        editForm.addEventListener("submit", event => {
            const userId = document.getElementById("edit-user-id").value;
            editForm.action = `/librarian/users/update/${encodeURIComponent(userId)}`;
        });
    }

    applyUserFilters();
});

function applyUserFilters() {
    const role = document.getElementById("role-filter")?.value || "";
    const rows = Array.from(document.querySelectorAll(".user-row"));
    let visibleCount = 0;

    rows.forEach(row => {
        const visible = !role || row.dataset.role === role;

        row.classList.toggle("d-none", !visible);
        if (visible) visibleCount += 1;
    });

    const empty = document.getElementById("empty-user-filter");
    if (empty) {
        empty.classList.toggle("d-none", visibleCount > 0 || rows.length === 0);
    }
}

function submitBackendSearch() {
    const keyword = document.getElementById("user-search")?.value.trim() || "";
    const url = new URL(window.location.href);

    if (keyword) {
        url.searchParams.set("keyword", keyword);
    } else {
        url.searchParams.delete("keyword");
    }

    window.location.href = url.toString();
}

function openDetailModal(row) {
    if (!row) return;

    LibraryUI.setText("detail-avatar", LibraryUI.getInitials(row.dataset.name));
    LibraryUI.setText("detail-name", row.dataset.name || row.dataset.username || "");
    LibraryUI.setText("detail-email", row.dataset.email || "");
    LibraryUI.setText("detail-role", formatRole(row.dataset.role));
    LibraryUI.setText("detail-created", row.dataset.createdLabel || "Chưa rõ");
    LibraryUI.setText("detail-borrows", row.dataset.totalBorrows || "0");

    bootstrap.Modal.getOrCreateInstance(document.getElementById("userDetailModal")).show();
}

function openEditModal(row) {
    if (!row) return;

    document.getElementById("edit-user-id").value = row.dataset.id || "";
    document.getElementById("edit-full-name").value = row.dataset.name || "";
    document.getElementById("edit-email").value = row.dataset.email || "";
    document.getElementById("edit-role").value = row.dataset.role || "READER";

    bootstrap.Modal.getOrCreateInstance(document.getElementById("editUserModal")).show();
}

function openDeleteModal(row) {
    if (!row) return;

    LibraryUI.setText("delete-user-name", row.dataset.name || row.dataset.username || "");
    const button = document.getElementById("confirm-delete-user");
    button.onclick = () => {
        const form = document.createElement("form");
        form.method = "post";
        form.action = `/librarian/users/delete/${encodeURIComponent(row.dataset.id || "")}`;
        document.body.appendChild(form);
        form.submit();
    };

    bootstrap.Modal.getOrCreateInstance(document.getElementById("deleteUserModal")).show();
}

function formatRole(role) {
    const labels = {
        READER: "Độc giả",
        LIBRARIAN: "Thủ thư",
        ADMIN: "Admin"
    };
    return labels[role] || role || "Độc giả";
}
