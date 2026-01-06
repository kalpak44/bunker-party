import { state } from './state.js';
import { $, escapeHtml, log } from './utils.js';
import { sendRevealCard, sendVoteEliminate } from './websocket.js';

export function renderCards() {
    const root = $("yourCards");
    root.innerHTML = "";

    const myPublic = state.gameState.revealed?.[state.myNameCanonical] || {};
    const inReveal = state.gameState.phase === "reveal";
    const meOut = (state.gameState.eliminated_names || []).some(n => (n || "").toLowerCase() === (state.myNameCanonical || "").toLowerCase());
    const canRevealThisRound = inReveal && !state.revealedThisRound && !meOut;

    Object.keys(state.character || {}).forEach((k) => {
        const alreadyUsed = myPublic[k] !== undefined;
        const disabled = alreadyUsed || !canRevealThisRound;

        const btn = document.createElement("button");
        btn.className =
            "bg-gray-900 p-4 rounded-lg border border-gray-700 text-left min-h-[88px] " +
            (disabled ? "opacity-50 cursor-not-allowed" : "hover:border-gray-500");

        btn.innerHTML = `
            <div class="text-xs text-gray-400">${escapeHtml(state.labels[k] || k)}</div>
            <div class="font-semibold text-base">${escapeHtml(state.character[k])}</div>
            <div class="text-xs text-gray-500 mt-1">
                ${escapeHtml(alreadyUsed ? state.ui.already_revealed : state.ui.hidden)}
            </div>
        `;

        btn.disabled = disabled;
        btn.onclick = () => {
            sendRevealCard(k);
            renderCards();
        };

        root.appendChild(btn);
    });
}

export function renderVote() {
    const list = $("voteList");
    const hint = $("voteHint");
    list.innerHTML = "";
    if (!state.gameState || state.gameState.phase !== "vote") {
        return;
    }
    const eliminated = new Set((state.gameState.eliminated_names || []).map(n => (n || "").toLowerCase()));
    const allPlayers = ($("players").textContent || "").split(": ")[1] || "";
    const names = allPlayers ? allPlayers.split(/,\s*/).filter(Boolean) : [];
    const active = names.filter(n => !eliminated.has((n || "").toLowerCase()));
    const meOut = eliminated.has((state.myNameCanonical || "").toLowerCase());
    const canVote = !meOut;
    const inRevote = Array.isArray(state.gameState.revote_targets) && state.gameState.revote_targets.length > 0;
    const revoteSet = new Set((state.gameState.revote_targets || []).map(n => (n || "").toLowerCase()));
    const effectiveQuota = (state.gameState.revote_quota && state.gameState.revote_quota > 0) ? state.gameState.revote_quota : state.gameState.vote_quota;
    if (inRevote) {
        hint.textContent = (state.ui.vote_hint || "Choose who to eliminate this round.") + ` — revote among tied players` + (effectiveQuota ? ` (${effectiveQuota} will leave)` : "");
    } else {
        hint.textContent = (state.ui.vote_hint || "Choose who to eliminate this round.") + (effectiveQuota ? ` (${effectiveQuota} will leave)` : "");
    }

    const displayNames = inRevote ? active.filter(n => revoteSet.has((n || "").toLowerCase())) : active;
    displayNames.forEach(name => {
        if ((name || "").toLowerCase() === (state.myNameCanonical || "").toLowerCase()) return; // cannot vote self
        const btn = document.createElement("button");
        btn.className = "min-h-[36px] bg-red-700 hover:bg-red-600 disabled:opacity-50 px-3 py-1 rounded text-sm";
        btn.textContent = name;
        btn.disabled = !canVote;
        btn.onclick = () => {
            sendVoteEliminate(name);
            // Optimistically disable buttons to prevent double votes
            Array.from(list.querySelectorAll("button")).forEach(b => b.disabled = true);
        };
        list.appendChild(btn);
    });
}

export function renderRevealed() {
    const root = $("revealedBoard");
    root.innerHTML = "";

    Object.entries(state.gameState.revealed || {}).forEach(([name, cards]) => {
        const box = document.createElement("div");
        box.className = "bg-gray-900 p-3 rounded border border-gray-700";

        let html = `<div class="font-semibold mb-2">${escapeHtml(name)}</div>`;
        const entries = Object.entries(cards || {});

        if (!entries.length) html += `<div class="text-sm text-gray-500">—</div>`;
        else entries.forEach(([k, v]) => {
            // Labels are already localized from the backend
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

export function renderPlayers() {
    if (!state.gameState.online_status) return;

    // Get player names from revealed state (all players)
    const playerNames = Object.keys(state.gameState.revealed || {});
    const onlineStatus = state.gameState.online_status || {};

    // Build player list with status indicators
    const playerList = playerNames.map(name => {
        const isOnline = onlineStatus[name] !== false;
        const indicator = isOnline ? "🟢" : "🔴";
        return `${indicator} ${name}`;
    }).join(", ");

    $("players").textContent = `${state.ui.players}: ${playerList}`;
}

