import { state } from './state.js';
import { $, getQueryParam, log } from './utils.js';
import { getSession } from './storage.js';
import { joinRoom } from './websocket.js';

// Auto-join/restore logic
export function initializeApp() {
    const roomParam = getQueryParam('room');
    const session = getSession();

    // Restore language preference
    if (session.lang) {
        $("lang").value = session.lang;
    }

    // Restore name if available
    if (session.name) {
        $("name").value = session.name;
    }

    // Auto-reconnect logic
    if (session.name && session.room) {
        // If URL has room param and it matches saved session, auto-reconnect
        if (roomParam && roomParam === session.room) {
            state.currentRoomId = session.room;
            state.myName = session.name;
            state.myLang = session.lang;

            // Automatically reconnect
            setTimeout(() => {
                joinRoom(session.room);
            }, 100);
            return;
        }

        // If no room param but we have a saved session, auto-reconnect
        if (!roomParam) {
            state.currentRoomId = session.room;
            state.myName = session.name;
            state.myLang = session.lang;

            // Automatically reconnect
            setTimeout(() => {
                joinRoom(session.room);
            }, 100);
            return;
        }
    }

    // If URL has room param (but no matching session), prepare to join that room
    if (roomParam && /^\d{4}$/.test(roomParam)) {
        state.currentRoomId = roomParam;
        // Update button text to show we're joining a specific room
        $("joinOrCreate").textContent = `Join room ${roomParam}`;
    }
}

// Clipboard functionality
export async function copyLinkToClipboard() {
    const link = $("shareLink").value;
    try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(link);
            log(state.ui.copied || "Copied");
        } else {
            // Fallback for browsers without clipboard API
            $("shareLink").select();
            $("shareLink").setSelectionRange(0, 99999); // For mobile devices
            document.execCommand('copy');
            log(state.ui.copied || "Copied");
        }
    } catch (err) {
        // If all fails, just select the text so user can copy manually
        $("shareLink").select();
        log("Please copy manually (Ctrl+C or Cmd+C)");
    }
}

// Join or create room
export async function handleJoinOrCreate() {
    const { clearMainError, showMainError } = await import('./utils.js');

    clearMainError();
    state.myName = $("name").value.trim();
    state.myLang = $("lang").value;

    if (!state.myName) return showMainError("Name is required");

    // If we have a room code from URL, join it
    if (state.currentRoomId) {
        joinRoom(state.currentRoomId);
        return;
    }

    // Otherwise, create a new room
    const res = await fetch(`/create/${state.myLang}`, { method: "POST" });
    const data = await res.json();

    state.currentRoomId = data.room_id;
    joinRoom(data.room_id);
}
