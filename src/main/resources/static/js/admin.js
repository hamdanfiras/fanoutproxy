document.querySelectorAll("[data-sortable]").forEach((list) => {
    let dragging = null;

    list.addEventListener("dragstart", (event) => {
        const row = event.target.closest("[draggable='true']");
        if (!row) {
            return;
        }
        dragging = row;
        row.classList.add("dragging");
        event.dataTransfer.effectAllowed = "move";
    });

    list.addEventListener("dragend", () => {
        if (dragging) {
            dragging.classList.remove("dragging");
        }
        dragging = null;
        updateOrder(list);
    });

    list.addEventListener("dragover", (event) => {
        event.preventDefault();
        const after = rowAfterPointer(list, event.clientY);
        if (!dragging) {
            return;
        }
        if (after == null) {
            list.appendChild(dragging);
        } else {
            list.insertBefore(dragging, after);
        }
    });

    updateOrder(list);
});

document.querySelectorAll("[data-reorder-form]").forEach((form) => {
    form.addEventListener("submit", () => {
        const source = form.getAttribute("data-reorder-source");
        const list = source ? document.getElementById(source) : form.querySelector("[data-sortable]");
        updateOrder(list);
    });
});

function rowAfterPointer(list, y) {
    const rows = [...list.querySelectorAll("[draggable='true']:not(.dragging)")];
    return rows.reduce((closest, row) => {
        const box = row.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) {
            return {offset, row};
        }
        return closest;
    }, {offset: Number.NEGATIVE_INFINITY, row: null}).row;
}

function updateOrder(list) {
    if (!list) {
        return;
    }
    const form = list.closest("[data-reorder-form]")
        || document.querySelector(`[data-reorder-form][data-reorder-source="${list.id}"]`);
    const field = form && form.querySelector("[data-ordered-ids]");
    if (!field) {
        return;
    }
    field.value = [...list.querySelectorAll("[data-id]")]
        .map((row) => row.getAttribute("data-id"))
        .join(",");
}
