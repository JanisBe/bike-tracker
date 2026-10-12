/**
 * Toast Notification Service
 */

let toastTimeout = null;

/**
 * Displays a toast notification in the bottom right corner
 * @param {string} message
 * @param {'info'|'success'|'error'} [type='info']
 */
export function showToast(message, type = "info") {
    const toastContainer = document.getElementById("toast-container");
    if (!toastContainer) return;

    toastContainer.innerHTML = "";
    if (toastTimeout) clearTimeout(toastTimeout);

    const toast = document.createElement("div");
    toast.className = `toast-notification toast-${type}`;

    let icon = "ℹ️";
    if (type === "success") icon = "✅";
    if (type === "error") icon = "⚠️";

    toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
    toastContainer.appendChild(toast);

    requestAnimationFrame(() => {
        toast.classList.add("active");
    });

    toastTimeout = setTimeout(() => {
        toast.classList.remove("active");
        setTimeout(() => toast.remove(), 300);
    }, 2800);
}
