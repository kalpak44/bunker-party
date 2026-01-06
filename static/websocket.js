import { state } from './state.js';
import { $, log, showMainError, protoWs } from './utils.js';
import { saveSession, clearSession } from './storage.js';
import { renderCards, renderRevealed, renderVote, renderPlayers, renderSkipButton } from './rendering.js';

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

function handleMessage(e) {
    const m = JSON.parse(e.data);

    if (m.type === "error") {
        const msg = m.message || state.ui.error || "Error";
        log("⚠️ " + msg);
        $("game").classList.add("hidden");
        $("main").classList.remove("hidden");
        showMainError(msg);

        // Clear session on error (e.g., room doesn't exist, game started, etc.)
        clearSession();

        try { state.ws.close(); } catch {}
        return;
    }

    if (m.type === "init") {
        handleInit(m);
        return;
    }

    if (m.type === "players") {
        // Simple list first, will be enriched with online status from state updates
        $("players").textContent = `${state.ui.players}: ${m.players.join(", ")}`;
        log(`${state.ui.players}: ${m.players.join(", ")}`);
    }

    if (m.type === "bunker_event") {
        state.hasStarted = true;
        state.revealedThisRound = false;
        $("round").textContent = `${state.ui.round}: ${m.round}`;
        $("bunker").textContent = `${state.ui.bunker_event}: ${m.event}`;
        log(`🏠 ${state.ui.round} ${m.round}: ${m.event}`);
        renderCards();
    }

    if (m.type === "player_reveal") {
        log(`🔓 ${m.player} — ${state.labels[m.key] || m.key}: ${m.value}`);
    }

    if (m.type === "player_reconnected") {
        log(`🔌 ${m.player} ${state.ui.player_reconnected || "reconnected"}`);
    }

    if (m.type === "player_disconnected") {
        log(`⚠️ ${m.player} ${state.ui.player_disconnected || "disconnected"}`);
    }

    if (m.type === "game_over") {
        $("gameOver").classList.remove("hidden");
        $("gameOver").textContent = m.message || state.ui.game_over;
        log("🏁 " + $("gameOver").textContent);
    }

    if (m.type === "state") {
        handleStateUpdate(m);
    }
}

function handleInit(m) {
    state.ui = m.ui;
    state.labels = m.labels;
    state.character = m.character;
    state.myNameCanonical = m.player_name;
    state.currentRoomId = m.room_id;

    $("title").textContent = state.ui.title;
    $("name").placeholder = state.ui.your_name;

    $("roomCodeLabel").textContent = state.ui.room_code_label;
    $("copyLinkBtn").textContent = state.ui.copy;
    $("logTitle").textContent = state.ui.log_title;
    $("yourCardsTitle").textContent = state.ui.your_cards;
    $("revealedCardsTitle").textContent = state.ui.revealed_cards;

    $("startBtn").textContent = state.ui.ready_to_start;
    $("confirm").textContent = state.ui.confirm_round_end;
    $("waiting").textContent = state.ui.waiting_opponents;
    $("voteTitle").textContent = state.ui.phase_vote || "Vote";

    $("roomCode").textContent = m.room_id;
    log(`${state.ui.room_code}: ${m.room_id}`);

    // Generate shareable link and QR code
    const shareableLink = `${location.protocol}//${location.host}/?room=${m.room_id}`;
    $("shareLink").value = shareableLink;

    // Clear previous QR code if any
    $("qrcode").innerHTML = "";
    new QRCode($("qrcode"), {
        text: shareableLink,
        width: 128,
        height: 128,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M
    });

    renderCards();
}

function handleStateUpdate(m) {
    state.gameState = m;

    let phaseText = state.ui.game_over;
    if (m.phase === "lobby") phaseText = state.ui.phase_lobby;
    if (m.phase === "reveal") phaseText = state.ui.phase_reveal;
    if (m.phase === "confirm") phaseText = state.ui.phase_confirm;
    if (m.phase === "vote") phaseText = state.ui.phase_vote || "Vote";
    $("phase").textContent = phaseText;

    // Hide start button if player already voted or not in lobby
    const showStartBtn = m.phase === "lobby" && !m.player_action_done;
    $("startBtn").classList.toggle("hidden", !showStartBtn);

    // Hide confirm button if player already confirmed, not in confirm phase, or game hasn't started
    const showConfirmBtn = m.phase === "confirm" && state.hasStarted && !m.player_action_done;
    $("confirm").classList.toggle("hidden", !showConfirmBtn);

    $("votePanel").classList.toggle("hidden", m.phase !== "vote");

    const waiting =
        (m.phase === "lobby" && m.start_votes < m.players_total) ||
        (m.phase === "confirm" && m.confirms_done < m.players_total) ||
        (m.phase === "vote" && (m.votes_done || 0) < (m.players_total || 0));

    $("waiting").classList.toggle("hidden", !waiting);
    $("waiting").textContent =
        m.phase === "lobby" ? (state.ui.waiting_votes || "Waiting for votes...") :
            m.phase === "confirm" ? (state.ui.waiting_confirms || "Waiting for confirmations...") :
                (state.ui.waiting_votes || "Waiting for votes...");

    const showMinPlayers = m.phase === "lobby" && (m.players_total || 0) < 3;
    $("minPlayersHint").classList.toggle("hidden", !showMinPlayers);
    if (showMinPlayers) {
        try {
            const tpl = state.ui.min_players_to_start || "Need at least {n} players to start";
            $("minPlayersHint").textContent = tpl.replace("{n}", "3");
        } catch {
            $("minPlayersHint").textContent = "Need at least 3 players to start";
        }
    }

    if (m.phase === "lobby")
        $("progress").textContent = `${state.ui.ready_to_start}: ${m.start_votes}/${m.players_total}`;
    else if (m.phase === "reveal")
        $("progress").textContent = `${state.ui.phase_reveal}: ${m.reveals_done}/${m.players_total}`;
    else if (m.phase === "confirm")
        $("progress").textContent = `${state.ui.phase_confirm}: ${m.confirms_done}/${m.players_total}`;
    else if (m.phase === "vote")
        $("progress").textContent = `${state.ui.phase_vote || "Vote"}: ${m.votes_done || 0}/${m.players_total}`;
    else $("progress").textContent = "";

    renderCards();
    renderRevealed();
    renderVote();
    renderPlayers();
    renderSkipButton();

    // Show eliminated state for me
    const meOut = (m.eliminated_names || []).some(n => (n || "").toLowerCase() === (state.myNameCanonical || "").toLowerCase());
    $("youOut").classList.toggle("hidden", !meOut);
    if (meOut) {
        $("youOut").textContent = state.ui.you_are_out || "You are out. You can observe but not participate.";
    }
}

export function sendVoteStart() {
    state.ws.send(JSON.stringify({ type: "vote_start" }));
    log("✅ " + (state.ui.you_voted_start || "Voted"));
}

export function sendConfirmRoundEnd() {
    state.ws.send(JSON.stringify({ type: "confirm_round_end" }));
    log("✅ " + (state.ui.you_confirmed || "Confirmed"));
}

export function sendRevealCard(key) {
    state.ws.send(JSON.stringify({ type: "reveal_card", key }));
    state.revealedThisRound = true;
    log(`🎴 ${state.ui.you_revealed}: ${state.labels[key] || key}`);
}

export function sendVoteEliminate(target) {
    state.ws.send(JSON.stringify({ type: "vote_eliminate", target }));
    log(`🗳️ ${state.ui.you_voted || "You voted"}: ${target}`);
}

export function sendSkipInactive() {
    state.ws.send(JSON.stringify({ type: "skip_inactive" }));
    log(`⏭️ ${state.ui.skip_inactive_player || "Voted to skip"}`);
}
