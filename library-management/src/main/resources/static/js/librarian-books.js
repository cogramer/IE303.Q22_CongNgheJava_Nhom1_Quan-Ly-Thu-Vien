document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("book-search");
    const categoryFilter = document.getElementById("category-filter");
    const deleteForm = document.getElementById("delete-book-form");
    const deleteTitle = document.getElementById("delete-book-title");

    [searchInput, categoryFilter].forEach(element => {
        if (element) {
            element.addEventListener("input", applyBookFilters);
        }
    });

    document.querySelectorAll(".delete-book-btn").forEach(button => {
        button.addEventListener("click", () => {
            deleteForm.action = button.dataset.deleteUrl;
            deleteTitle.textContent = button.dataset.bookTitle || "này";
        });
    });

    applyBookFilters();
});

function applyBookFilters() {
    const keyword = LibraryUI.normalizeText(document.getElementById("book-search").value);
    const category = document.getElementById("category-filter").value;
    const rows = Array.from(document.querySelectorAll(".book-row"));
    let visibleCount = 0;

    rows.forEach(row => {
        const searchable = LibraryUI.normalizeText(`${row.dataset.title} ${row.dataset.author}`);
        const matchesKeyword = searchable.includes(keyword);
        const matchesCategory = !category || row.dataset.category === category;
        const visible = matchesKeyword && matchesCategory;

        row.classList.toggle("d-none", !visible);
        if (visible) visibleCount++;
    });

    document.getElementById("empty-filter-state").classList.toggle("d-none", visibleCount > 0 || rows.length === 0);
}
