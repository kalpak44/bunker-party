let ws;
let ui = {};
let labels = {};
let character = {};
let state = {};
let myName = "";
let myLang = "en";
let myNameCanonical = "";
let hasStarted = false;
let revealedThisRound = false;
let currentRoomId = "";

const $ = (id) => document.getElementById(id);

// Parse query parameters on load
function getQueryParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
}

// Auto-join logic
window.addEventListener('DOMContentLoaded', () => {
    const roomParam = getQueryParam('room');
    if (roomParam && /^\d{4}$/.test(roomParam)) {
        $("room").value = roomParam;
    }
});

function showMainError(text) {
    $("mainError").classList.remove("hidden");
    $("mainError").textContent = text;
}

function clearMainError() {
    $("mainError").classList.add("hidden");
    $("mainError").textContent = "";
}

function log(msg) {
    const d = document.createElement("div");
    d.textContent = msg;
    $("log").appendChild(d);
    $("log").scrollTop = $("log").scrollHeight;
}

function escapeHtml(s) {
    return String(s)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function protoWs() {
    return location.protocol === "https:" ? "wss" : "ws";
}

$("copyBtn").onclick = async () => {
    await navigator.clipboard.writeText($("roomCode").textContent);
    log(ui.copied || "Copied");
};

$("copyLinkBtn").onclick = async () => {
    await navigator.clipboard.writeText($("shareLink").value);
    log("Link copied!");
};

$("create").onclick = async () => {
    clearMainError();
    myName = $("name").value.trim();
    myLang = $("lang").value;

    if (!myName) return showMainError("Name is required");

    const res = await fetch(`/create/${myLang}`, { method: "POST" });
    const data = await res.json();

    $("room").value = data.room_id;
    joinRoom(data.room_id);
};

$("join").onclick = () => {
    clearMainError();
    myName = $("name").value.trim();
    myLang = $("lang").value;
    const room = $("room").value.trim();

    if (!myName) return showMainError("Name is required");
    if (!/^\d{4}$/.test(room)) return showMainError("Room code must be 4 digits");

    joinRoom(room);
};

function joinRoom(room) {
    const wsUrl = `${protoWs()}://${location.host}/ws/${room}`;
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
        ws.send(JSON.stringify({ name: myName, lang: myLang }));
        $("main").classList.add("hidden");
        $("game").classList.remove("hidden");
    };

    ws.onclose = () => log(ui.disconnected || "Disconnected");

    ws.onmessage = (e) => {
        const m = JSON.parse(e.data);

        if (m.type === "error") {
            const msg = m.message || ui.error || "Error";
            log("⚠️ " + msg);
            $("game").classList.add("hidden");
            $("main").classList.remove("hidden");
            showMainError(msg);
            try { ws.close(); } catch {}
            return;
        }

        if (m.type === "init") {
            ui = m.ui;
            labels = m.labels;
            character = m.character;
            myNameCanonical = m.player_name;
            currentRoomId = m.room_id;

            $("title").textContent = ui.title;
            $("create").textContent = ui.create_new_game;
            $("join").textContent = ui.join;
            $("name").placeholder = ui.your_name;
            $("room").placeholder = ui.room_code_placeholder;

            $("roomCodeLabel").textContent = ui.room_code_label;
            $("copyBtn").textContent = ui.copy;
            $("logTitle").textContent = ui.log_title;
            $("yourCardsTitle").textContent = ui.your_cards;
            $("revealedCardsTitle").textContent = ui.revealed_cards;

            $("startBtn").textContent = ui.ready_to_start;
            $("confirm").textContent = ui.confirm_round_end;
            $("waiting").textContent = ui.waiting_opponents;
            $("voteTitle").textContent = ui.phase_vote || "Vote";

            $("roomCode").textContent = m.room_id;
            log(`${ui.room_code}: ${m.room_id}`);

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

        if (m.type === "players") {
            $("players").textContent = `${ui.players}: ${m.players.join(", ")}`;
            log(`${ui.players}: ${m.players.join(", ")}`);
        }

        if (m.type === "bunker_event") {
            hasStarted = true;
            revealedThisRound = false;
            $("round").textContent = `${ui.round}: ${m.round}`;
            $("bunker").textContent = `${ui.bunker_event}: ${m.event}`;
            log(`🏠 ${ui.round} ${m.round}: ${m.event}`);
            renderCards();
        }

        if (m.type === "player_reveal") {
            log(`🔓 ${m.player} — ${labels[m.key] || m.key}: ${m.value}`);
        }

        if (m.type === "game_over") {
            $("gameOver").classList.remove("hidden");
            $("gameOver").textContent = m.message || ui.game_over;
            log("🏁 " + $("gameOver").textContent);
        }

        if (m.type === "state") {
            state = m;

            let phaseText = ui.game_over;
            if (state.phase === "lobby") phaseText = ui.phase_lobby;
            if (state.phase === "reveal") phaseText = ui.phase_reveal;
            if (state.phase === "confirm") phaseText = ui.phase_confirm;
            if (state.phase === "vote") phaseText = ui.phase_vote || "Vote";
            $("phase").textContent = phaseText;

            $("startBtn").classList.toggle("hidden", state.phase !== "lobby");
            $("confirm").classList.toggle("hidden", state.phase !== "confirm" || !hasStarted);
            $("votePanel").classList.toggle("hidden", state.phase !== "vote");

            const waiting =
                (state.phase === "lobby" && state.start_votes < state.players_total) ||
                (state.phase === "confirm" && state.confirms_done < state.players_total) ||
                (state.phase === "vote" && (state.votes_done || 0) < (state.players_total || 0));

            $("waiting").classList.toggle("hidden", !waiting);
            $("waiting").textContent =
                state.phase === "lobby" ? (ui.waiting_votes || "Waiting for votes...") :
                    state.phase === "confirm" ? (ui.waiting_confirms || "Waiting for confirmations...") :
                        (ui.waiting_votes || "Waiting for votes...");

            const showMinPlayers = state.phase === "lobby" && (state.players_total || 0) < 3;
            $("minPlayersHint").classList.toggle("hidden", !showMinPlayers);
            if (showMinPlayers) {
                try {
                    const tpl = ui.min_players_to_start || "Need at least {n} players to start";
                    $("minPlayersHint").textContent = tpl.replace("{n}", "3");
                } catch {
                    $("minPlayersHint").textContent = "Need at least 3 players to start";
                }
            }

            if (state.phase === "lobby")
                $("progress").textContent = `${ui.ready_to_start}: ${state.start_votes}/${state.players_total}`;
            else if (state.phase === "reveal")
                $("progress").textContent = `${ui.phase_reveal}: ${state.reveals_done}/${state.players_total}`;
            else if (state.phase === "confirm")
                $("progress").textContent = `${ui.phase_confirm}: ${state.confirms_done}/${state.players_total}`;
            else if (state.phase === "vote")
                $("progress").textContent = `${ui.phase_vote || "Vote"}: ${state.votes_done || 0}/${state.players_total}`;
            else $("progress").textContent = "";

            $("startBtn").disabled = false;
            $("confirm").disabled = false;

            renderCards();
            renderRevealed();
            renderVote();

            // Show eliminated state for me
            const meOut = (state.eliminated_names || []).some(n => (n || "").toLowerCase() === (myNameCanonical || "").toLowerCase());
            $("youOut").classList.toggle("hidden", !meOut);
            if (meOut) {
                $("youOut").textContent = ui.you_are_out || "You are out. You can observe but not participate.";
            }
        }
    };
}

$("startBtn").onclick = () => {
    ws.send(JSON.stringify({ type: "vote_start" }));
    $("startBtn").disabled = true;
    log("✅ " + (ui.you_voted_start || "Voted"));
};

$("confirm").onclick = () => {
    ws.send(JSON.stringify({ type: "confirm_round_end" }));
    $("confirm").disabled = true;
    log("✅ " + (ui.you_confirmed || "Confirmed"));
};

function renderCards() {
    const root = $("yourCards");
    root.innerHTML = "";

    const myPublic = state.revealed?.[myNameCanonical] || {};
    const inReveal = state.phase === "reveal";
    const meOut = (state.eliminated_names || []).some(n => (n || "").toLowerCase() === (myNameCanonical || "").toLowerCase());
    const canRevealThisRound = inReveal && !revealedThisRound && !meOut;

    Object.keys(character || {}).forEach((k) => {
        const alreadyUsed = myPublic[k] !== undefined;
        const disabled = alreadyUsed || !canRevealThisRound;

        const btn = document.createElement("button");
        btn.className =
            "bg-gray-900 p-4 rounded-lg border border-gray-700 text-left min-h-[88px] " +
            (disabled ? "opacity-50 cursor-not-allowed" : "hover:border-gray-500");

        btn.innerHTML = `
            <div class="text-xs text-gray-400">${escapeHtml(labels[k] || k)}</div>
            <div class="font-semibold text-base">${escapeHtml(character[k])}</div>
            <div class="text-xs text-gray-500 mt-1">
                ${escapeHtml(alreadyUsed ? ui.already_revealed : ui.hidden)}
            </div>
        `;

        btn.disabled = disabled;
        btn.onclick = () => {
            ws.send(JSON.stringify({ type: "reveal_card", key: k }));
            revealedThisRound = true;
            log(`🎴 ${ui.you_revealed}: ${labels[k] || k}`);
            renderCards();
        };

        root.appendChild(btn);
    });
}

function renderVote() {
    const list = $("voteList");
    const hint = $("voteHint");
    list.innerHTML = "";
    if (!state || state.phase !== "vote") {
        return;
    }
    const eliminated = new Set((state.eliminated_names || []).map(n => (n || "").toLowerCase()));
    const allPlayers = ($("players").textContent || "").split(": ")[1] || "";
    const names = allPlayers ? allPlayers.split(/,\s*/).filter(Boolean) : [];
    const active = names.filter(n => !eliminated.has((n || "").toLowerCase()));
    const meOut = eliminated.has((myNameCanonical || "").toLowerCase());
    const canVote = !meOut;
    const inRevote = Array.isArray(state.revote_targets) && state.revote_targets.length > 0;
    const revoteSet = new Set((state.revote_targets || []).map(n => (n || "").toLowerCase()));
    const effectiveQuota = (state.revote_quota && state.revote_quota > 0) ? state.revote_quota : state.vote_quota;
    if (inRevote) {
        hint.textContent = (ui.vote_hint || "Choose who to eliminate this round.") + ` — revote among tied players` + (effectiveQuota ? ` (${effectiveQuota} will leave)` : "");
    } else {
        hint.textContent = (ui.vote_hint || "Choose who to eliminate this round.") + (effectiveQuota ? ` (${effectiveQuota} will leave)` : "");
    }

    const displayNames = inRevote ? active.filter(n => revoteSet.has((n || "").toLowerCase())) : active;
    displayNames.forEach(name => {
        if ((name || "").toLowerCase() === (myNameCanonical || "").toLowerCase()) return; // cannot vote self
        const btn = document.createElement("button");
        btn.className = "min-h-[36px] bg-red-700 hover:bg-red-600 disabled:opacity-50 px-3 py-1 rounded text-sm";
        btn.textContent = name;
        btn.disabled = !canVote;
        btn.onclick = () => {
            ws.send(JSON.stringify({ type: "vote_eliminate", target: name }));
            // Optimistically disable buttons to prevent double votes
            Array.from(list.querySelectorAll("button")).forEach(b => b.disabled = true);
            log(`🗳️ ${ui.you_voted || "You voted"}: ${name}`);
        };
        list.appendChild(btn);
    });
}

function renderRevealed() {
    const root = $("revealedBoard");
    root.innerHTML = "";

    Object.entries(state.revealed || {}).forEach(([name, cards]) => {
        const box = document.createElement("div");
        box.className = "bg-gray-900 p-3 rounded border border-gray-700";

        let html = `<div class="font-semibold mb-2">${escapeHtml(name)}</div>`;
        const entries = Object.entries(cards || {});

        if (!entries.length) html += `<div class="text-sm text-gray-500">—</div>`;
        else entries.forEach(([k, v]) => {
            html += `
                <div class="text-sm flex justify-between gap-3">
                    <div class="text-gray-400">${escapeHtml(labels[k] || k)}</div>
                    <div class="font-medium">${escapeHtml(v)}</div>
                </div>`;
        });

        box.innerHTML = html;
        root.appendChild(box);
    });
}
