import { state } from './state.js';
import { $, escapeHtml, log } from './utils.js';
import { sendRevealCard, sendVoteEliminate, getUI, getCharacter, getLogs, getPlayers, getRevealed, getGameState } from './websocket.js';

// Main render function - fetches all data and renders everything
export async function renderAll() {
    try {
        // Fetch all data in parallel
        const [uiData, characterData, logsData, playersData, revealedData, gameStateData] = await Promise.all([
            getUI(),
            getCharacter(),
            getLogs(),
            getPlayers(),
            getRevealed(),
            getGameState()
        ]);

        // Render each section
        renderPhaseAndProgress(uiData, gameStateData);
        renderCards(uiData, characterData, gameStateData, revealedData);
        renderRevealed(revealedData);
        renderVote(uiData, gameStateData, playersData);
        renderPlayers(uiData, playersData);
        renderLogs(logsData);
        renderGameStatus(uiData, gameStateData);
    } catch (error) {
        console.error('Error rendering:', error);
    }
}

function renderPhaseAndProgress(uiData, gameStateData) {
    const ui = uiData.ui;
    const gs = gameStateData;

    let phaseText = ui.game_over;
    if (gs.phase === "lobby") phaseText = ui.phase_lobby;
    if (gs.phase === "reveal") phaseText = ui.phase_reveal;
    if (gs.phase === "confirm") phaseText = ui.phase_confirm;
    if (gs.phase === "vote") phaseText = ui.phase_vote || "Vote";
    $("phase").textContent = phaseText;

    // Show bunker event if round started
    if (gs.round > 0 && gs.event_idx !== null) {
        $("round").textContent = `${ui.round}: ${gs.round}`;
        const bunkerEvent = uiData.ui.bunkers?.[gs.event_idx] || "";
        if (bunkerEvent) {
            $("bunker").textContent = `${ui.bunker_event}: ${bunkerEvent}`;
        }
    }

    // Progress text
    if (gs.phase === "lobby")
        $("progress").textContent = `${ui.ready_to_start}: ${gs.start_votes}/${gs.players_total}`;
    else if (gs.phase === "reveal")
        $("progress").textContent = `${ui.phase_reveal}: ${gs.reveals_done}/${gs.players_total}`;
    else if (gs.phase === "confirm")
        $("progress").textContent = `${ui.phase_confirm}: ${gs.confirms_done}/${gs.players_total}`;
    else if (gs.phase === "vote")
        $("progress").textContent = `${ui.phase_vote || "Vote"}: ${gs.votes_done || 0}/${gs.players_total}`;
    else $("progress").textContent = "";

    // Button visibility
    const showStartBtn = gs.phase === "lobby" && !gs.player_action_done;
    $("startBtn").classList.toggle("hidden", !showStartBtn);

    const showConfirmBtn = gs.phase === "confirm" && !gs.player_action_done && gs.round > 0;
    $("confirm").classList.toggle("hidden", !showConfirmBtn);

    $("votePanel").classList.toggle("hidden", gs.phase !== "vote");

    const waiting =
        (gs.phase === "lobby" && gs.start_votes < gs.players_total) ||
        (gs.phase === "confirm" && gs.confirms_done < gs.players_total) ||
        (gs.phase === "vote" && (gs.votes_done || 0) < (gs.players_total || 0));

    $("waiting").classList.toggle("hidden", !waiting);
    $("waiting").textContent =
        gs.phase === "lobby" ? (ui.waiting_votes || "Waiting for votes...") :
            gs.phase === "confirm" ? (ui.waiting_confirms || "Waiting for confirmations...") :
                (ui.waiting_votes || "Waiting for votes...");

    const showMinPlayers = gs.phase === "lobby" && (gs.players_total || 0) < 3;
    $("minPlayersHint").classList.toggle("hidden", !showMinPlayers);
    if (showMinPlayers) {
        try {
            const tpl = ui.min_players_to_start || "Need at least {n} players to start";
            $("minPlayersHint").textContent = tpl.replace("{n}", "3");
        } catch {
            $("minPlayersHint").textContent = "Need at least 3 players to start";
        }
    }
}

function renderCards(uiData, characterData, gameStateData, revealedData) {
    const ui = uiData.ui;
    const labels = uiData.labels;
    const character = characterData.character;
    const myRevealed = revealedData.revealed[state.myName] || {};
    const inReveal = gameStateData.phase === "reveal";
    const meOut = (gameStateData.eliminated_names || []).some(n => n.toLowerCase() === state.myName.toLowerCase());
    const canRevealThisRound = inReveal && !gameStateData.player_action_done && !meOut;

    const root = $("yourCards");
    root.innerHTML = "";

    Object.keys(character || {}).forEach((rawKey) => {
        // Check if already revealed using localized key
        const localizedKey = labels[rawKey] || rawKey;
        const alreadyUsed = myRevealed[localizedKey] !== undefined;
        const disabled = alreadyUsed || !canRevealThisRound;

        const btn = document.createElement("button");
        btn.className =
            "bg-gray-900 p-4 rounded-lg border border-gray-700 text-left min-h-[88px] " +
            (disabled ? "opacity-50 cursor-not-allowed" : "hover:border-gray-500");

        btn.innerHTML = `
            <div class="text-xs text-gray-400">${escapeHtml(localizedKey)}</div>
            <div class="font-semibold text-base">${escapeHtml(character[rawKey])}</div>
            <div class="text-xs text-gray-500 mt-1">
                ${escapeHtml(alreadyUsed ? ui.already_revealed : ui.hidden)}
            </div>
        `;

        btn.disabled = disabled;
        btn.onclick = () => {
            sendRevealCard(rawKey);
        };

        root.appendChild(btn);
    });
}

function renderRevealed(revealedData) {
    const root = $("revealedBoard");
    root.innerHTML = "";

    Object.entries(revealedData.revealed || {}).forEach(([name, cards]) => {
        const box = document.createElement("div");
        box.className = "bg-gray-900 p-3 rounded border border-gray-700";

        let html = `<div class="font-semibold mb-2">${escapeHtml(name)}</div>`;
        const entries = Object.entries(cards || {});

        if (!entries.length) html += `<div class="text-sm text-gray-500">—</div>`;
        else entries.forEach(([k, v]) => {
            html += `
                <div class="text-sm flex justify-between gap-3">
                    <div class="text-gray-400">${escapeHtml(k)}</div>
                    <div class="font-medium">${escapeHtml(v)}</div>
                </div>`;
        });

        box.innerHTML = html;
        root.appendChild(box);
    });
}

function renderVote(uiData, gameStateData, playersData) {
    const list = $("voteList");
    const hint = $("voteHint");
    list.innerHTML = "";

    if (!gameStateData || gameStateData.phase !== "vote") {
        return;
    }

    const ui = uiData.ui;
    const eliminated = new Set((gameStateData.eliminated_names || []).map(n => n.toLowerCase()));
    const allPlayers = playersData.players.map(p => p.name);
    const active = allPlayers.filter(n => !eliminated.has(n.toLowerCase()));
    const meOut = eliminated.has(state.myName.toLowerCase());
    const canVote = !meOut && !gameStateData.player_action_done;

    const inRevote = Array.isArray(gameStateData.revote_targets) && gameStateData.revote_targets.length > 0;
    const revoteSet = new Set((gameStateData.revote_targets || []).map(n => n.toLowerCase()));
    const effectiveQuota = (gameStateData.revote_quota && gameStateData.revote_quota > 0) ?
        gameStateData.revote_quota : gameStateData.vote_quota;

    if (inRevote) {
        hint.textContent = (ui.vote_hint || "Choose who to eliminate this round.") +
            ` — revote among tied players` + (effectiveQuota ? ` (${effectiveQuota} will leave)` : "");
    } else {
        hint.textContent = (ui.vote_hint || "Choose who to eliminate this round.") +
            (effectiveQuota ? ` (${effectiveQuota} will leave)` : "");
    }

    const displayNames = inRevote ? active.filter(n => revoteSet.has(n.toLowerCase())) : active;
    displayNames.forEach(name => {
        if (name.toLowerCase() === state.myName.toLowerCase()) return; // cannot vote self

        const btn = document.createElement("button");
        btn.className = "min-h-[36px] bg-red-700 hover:bg-red-600 disabled:opacity-50 px-3 py-1 rounded text-sm";
        btn.textContent = name;
        btn.disabled = !canVote;
        btn.onclick = () => {
            sendVoteEliminate(name);
            // Optimistically disable buttons
            Array.from(list.querySelectorAll("button")).forEach(b => b.disabled = true);
        };
        list.appendChild(btn);
    });
}

function renderPlayers(uiData, playersData) {
    const ui = uiData.ui;
    const players = playersData.players || [];

    const playerList = players.map(p => {
        const indicator = p.online ? "🟢" : "🔴";
        return `${indicator} ${p.name}`;
    }).join(", ");

    $("players").textContent = `${ui.players}: ${playerList}`;
}

function renderLogs(logsData) {
    const logs = logsData.logs || [];
    // Only show new logs that haven't been displayed yet
    const newLogs = logs.slice(state.displayedLogCount);
    newLogs.forEach(logMsg => {
        log(logMsg);
    });
    state.displayedLogCount = logs.length;
}

function renderGameStatus(uiData, gameStateData) {
    const ui = uiData.ui;

    // Show eliminated state for me
    const meOut = (gameStateData.eliminated_names || []).some(n => n.toLowerCase() === state.myName.toLowerCase());
    $("youOut").classList.toggle("hidden", !meOut);
    if (meOut) {
        $("youOut").textContent = ui.you_are_out || "You are out. You can observe but not participate.";
    }

    // Show game over if applicable
    if (gameStateData.phase === "game_over") {
        $("gameOver").classList.remove("hidden");
        // The game over message will be in the logs
    } else {
        $("gameOver").classList.add("hidden");
    }
}
