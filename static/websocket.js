import { state } from './state.js';
import { $, log, showMainError, protoWs } from './utils.js';
import { saveSession, clearSession } from './storage.js';
import { renderAll } from './rendering.js';

// Request ID counter for request/response pattern
let requestId = 0;

// Helper to send a request and wait for response
function sendRequest(type, data = {}) {
    return new Promise((resolve, reject) => {
        const id = ++requestId;
        state.pendingRequests.set(id, { resolve, reject });

        // Timeout after 5 seconds
        setTimeout(() => {
            if (state.pendingRequests.has(id)) {
                state.pendingRequests.delete(id);
                reject(new Error('Request timeout'));
            }
        }, 5000);

        state.ws.send(JSON.stringify({ ...data, type, requestId: id }));
    });
}

// Exported request functions
export async function getUI() {
    return sendRequest('get_ui');
}

export async function getCharacter() {
    return sendRequest('get_character');
}

export async function getLogs() {
    return sendRequest('get_logs');
}

export async function getPlayers() {
    return sendRequest('get_players');
}

export async function getRevealed() {
    return sendRequest('get_revealed');
}

export async function getGameState() {
    return sendRequest('get_game_state');
}

export function joinRoom(room) {
    const wsUrl = `${protoWs()}://${location.host}/ws/${room}`;
    state.ws = new WebSocket(wsUrl);

    state.ws.onopen = () => {
        state.ws.send(JSON.stringify({ name: state.myName, lang: state.myLang }));

        // Save session to localStorage on successful connection
        saveSession(state.myName, room, state.myLang);

        // Reset reconnect attempts on successful connection
        state.reconnectAttempts = 0;

        $("main").classList.add("hidden");
        $("game").classList.remove("hidden");
    };

    state.ws.onclose = () => {
        log(state.ui.disconnected || "Disconnected");
        // Don't clear session on disconnect - allow reconnection

        // Attempt to reconnect after a short delay, with a maximum number of attempts
        if (state.reconnectAttempts < state.MAX_RECONNECT_ATTEMPTS) {
            state.reconnectAttempts++;
            setTimeout(() => {
                if (state.myName && state.currentRoomId) {
                    log(state.ui.reconnecting || "Reconnecting...");
                    joinRoom(state.currentRoomId);
                }
            }, 2000);
        } else {
            log("Failed to reconnect. Please refresh the page.");
            clearSession();
            // Show the main screen
            $("game").classList.add("hidden");
            $("main").classList.remove("hidden");
        }
    };

    state.ws.onerror = () => {
        // Clear session on connection error
        clearSession();
    };

    state.ws.onmessage = handleMessage;
}

async function handleMessage(e) {
    const m = JSON.parse(e.data);

    // Handle request/response pattern
    if (m.requestId && state.pendingRequests.has(m.requestId)) {
        const { resolve } = state.pendingRequests.get(m.requestId);
        state.pendingRequests.delete(m.requestId);
        resolve(m);
        return;
    }

    if (m.type === "error") {
        const msg = m.message || "Error";
        log("⚠️ " + msg);
        $("game").classList.add("hidden");
        $("main").classList.remove("hidden");
        showMainError(msg);
        clearSession();
        try { state.ws.close(); } catch {}
        return;
    }

    if (m.type === "init") {
        handleInit(m);
        return;
    }

    // Handle refresh notification - fetch and re-render everything
    if (m.type === "refresh") {
        await renderAll();
    }
}

async function handleInit(m) {
    state.currentRoomId = m.room_id;

    const ui = m.ui;
    const labels = m.labels;

    // Set up UI text
    $("title").textContent = ui.title;
    $("name").placeholder = ui.your_name;
    $("roomCodeLabel").textContent = ui.room_code_label;
    $("copyLinkBtn").textContent = ui.copy;
    $("logTitle").textContent = ui.log_title;
    $("yourCardsTitle").textContent = ui.your_cards;
    $("revealedCardsTitle").textContent = ui.revealed_cards;
    $("startBtn").textContent = ui.ready_to_start;
    $("confirm").textContent = ui.confirm_round_end;
    $("waiting").textContent = ui.waiting_opponents;
    $("voteTitle").textContent = ui.phase_vote || "Vote";

    $("roomCode").textContent = m.room_id;

    // Generate shareable link and QR code
    const shareableLink = `${location.protocol}//${location.host}/?room=${m.room_id}`;
    $("shareLink").value = shareableLink;
    $("qrcode").innerHTML = "";
    new QRCode($("qrcode"), {
        text: shareableLink,
        width: 128,
        height: 128,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M
    });

    // Initial render
    await renderAll();
}

export function sendVoteStart() {
    state.ws.send(JSON.stringify({ type: "vote_start" }));
}

export function sendConfirmRoundEnd() {
    state.ws.send(JSON.stringify({ type: "confirm_round_end" }));
}

export function sendRevealCard(key) {
    state.ws.send(JSON.stringify({ type: "reveal_card", key }));
}

export function sendVoteEliminate(target) {
    state.ws.send(JSON.stringify({ type: "vote_eliminate", target }));
}

