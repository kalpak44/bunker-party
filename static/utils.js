// Utility functions
export const $ = (id) => document.getElementById(id);

export function getQueryParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
}

export function log(msg) {
    const d = document.createElement("div");
    d.textContent = msg;
    $("log").appendChild(d);
    $("log").scrollTop = $("log").scrollHeight;
}

export function escapeHtml(s) {
    return String(s)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

export function protoWs() {
    return location.protocol === "https:" ? "wss" : "ws";
}

export function showMainError(text) {
    $("mainError").classList.remove("hidden");
    $("mainError").textContent = text;
}

export function clearMainError() {
    $("mainError").classList.add("hidden");
    $("mainError").textContent = "";
}
