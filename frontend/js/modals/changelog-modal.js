/**
 * Modal dialog for displaying the Bike Tracker changelog.
 * Fetches CHANGELOG.md from GitHub Pages (or local) and parses Markdown into styled HTML.
 */

let isLoaded = false;
let changelogMarkdown = "";

const changelogModal = document.getElementById("changelog-modal");
const btnChangelog = document.getElementById("btn-changelog");
const btnChangelogClose = document.getElementById("btn-changelog-close");
const btnChangelogDismiss = document.getElementById("btn-changelog-dismiss");
const changelogContent = document.getElementById("changelog-content");

export function isChangelogModalOpen() {
    return changelogModal && changelogModal.style.display !== "none" && changelogModal.style.display !== "";
}

export function openChangelogModal() {
    if (!changelogModal) return;
    changelogModal.style.display = "flex";
    document.body.style.overflow = "hidden";
    if (!isLoaded) {
        void fetchAndRenderChangelog();
    }
}

export function closeChangelogModal() {
    if (!changelogModal) return;
    changelogModal.style.display = "none";
    document.body.style.overflow = "";
}

/**
 * Converts standard Keep-a-Changelog Markdown format to styled HTML.
 * @param {string} markdown
 * @returns {string} HTML string
 */
export function parseMarkdownToHtml(markdown) {
    const escapeHtml = (text) => text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");

    const lines = markdown.split("\n");
    let html = "";
    let inList = false;

    for (const rawLine of lines) {
        const line = rawLine.trim();

        if (line.startsWith("# ")) {
            // Main title - skipped as modal already has header
            continue;
        }

        if (line.startsWith("## ")) {
            if (inList) {
                html += "</ul>";
                inList = false;
            }
            const versionMatch = line.match(/^##\s+\[(.*?)\](?:\s+-\s+(.*))?/);
            if (versionMatch) {
                const version = escapeHtml(versionMatch[1] || "");
                const date = escapeHtml(versionMatch[2] || "");
                html += `
                    <div class="changelog-version-header">
                        <span class="changelog-version-tag">v${version}</span>
                        ${date ? `<span class="changelog-date">${date}</span>` : ""}
                    </div>
                `;
            } else {
                html += `<h3 class="changelog-version-title">${escapeHtml(line.slice(3))}</h3>`;
            }
            continue;
        }

        if (line.startsWith("### ")) {
            if (inList) {
                html += "</ul>";
                inList = false;
            }
            const category = line.slice(4).trim();
            const isAdded = category.toLowerCase().includes("dodano") || category.toLowerCase().includes("added");
            const isFixed = category.toLowerCase().includes("poprawiono") || category.toLowerCase().includes("fixed") || category.toLowerCase().includes("zmieniono");
            const categoryClass = isAdded ? "cat-added" : (isFixed ? "cat-fixed" : "cat-other");
            html += `<div class="changelog-category-badge ${categoryClass}">${escapeHtml(category)}</div>`;
            continue;
        }

        if (line.startsWith("- ") || line.startsWith("* ")) {
            if (!inList) {
                html += `<ul class="changelog-list">`;
                inList = true;
            }
            let itemText = line.slice(2).trim();
            itemText = itemText.replace(/\*\*(.*?)\*\*/g, (_, p1) => `<strong>${escapeHtml(p1)}</strong>`);
            html += `<li>${itemText}</li>`;
            continue;
        }

        if (line === "") {
            if (inList) {
                html += "</ul>";
                inList = false;
            }
            continue;
        }

        if (inList) {
            html += "</ul>";
            inList = false;
        }
        html += `<p class="changelog-paragraph">${escapeHtml(line)}</p>`;
    }

    if (inList) {
        html += "</ul>";
    }

    return html;
}

/**
 * Fetches the CHANGELOG.md file and renders it into the modal container.
 */
export async function fetchAndRenderChangelog() {
    try {
        let response = null;
        try {
            response = await fetch("CHANGELOG.md?v=" + Date.now());
        } catch (_) {
            // Relative fetch failed in local or dev environment
        }

        if (!response || !response.ok) {
            response = await fetch("https://janisbe.github.io/bike-tracker/CHANGELOG.md?v=" + Date.now());
        }

        if (!response?.ok) {
            throw new Error(`HTTP ${response?.status || 'Network error'}`);
        }

        changelogMarkdown = await response.text();
        isLoaded = true;

        if (changelogContent) {
            changelogContent.innerHTML = parseMarkdownToHtml(changelogMarkdown);
        }

        // Update footer button with latest detected version
        const firstVersionMatch = changelogMarkdown.match(/##\s+\[(.*?)\]/);
        if (firstVersionMatch?.[1] && btnChangelog) {
            const latestVer = firstVersionMatch[1];
            btnChangelog.textContent = `Bike Tracker v${latestVer} • Changelog`;
        }
    } catch (err) {
        console.error("Failed to fetch changelog:", err);
        if (changelogContent) {
            changelogContent.innerHTML = `
                <div class="changelog-error">
                    <span style="font-size: 26px;">⚠️</span>
                    <p>Nie udało się pobrać historii zmian.</p>
                    <button type="button" class="btn-retry" id="btn-changelog-retry">Spróbuj ponownie</button>
                </div>
            `;
            document.getElementById("btn-changelog-retry")?.addEventListener("click", () => {
                changelogContent.innerHTML = `
                    <div class="changelog-loading">
                        <span class="btn-spinner"></span>
                        <span>Wczytywanie historii zmian...</span>
                    </div>
                `;
                void fetchAndRenderChangelog();
            });
        }
    }
}

/**
 * Initializes listeners for the changelog modal.
 */
export function initChangelogModal() {
    btnChangelog?.addEventListener("click", () => {
        openChangelogModal();
    });

    btnChangelogClose?.addEventListener("click", () => {
        closeChangelogModal();
    });

    btnChangelogDismiss?.addEventListener("click", () => {
        closeChangelogModal();
    });

    changelogModal?.addEventListener("click", (e) => {
        if (e.target === changelogModal) {
            closeChangelogModal();
        }
    });

    // Pre-fetch changelog to set footer version text
    void fetchAndRenderChangelog();
}
