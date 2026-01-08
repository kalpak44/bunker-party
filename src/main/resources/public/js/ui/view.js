import { t } from '../core/i18n.js';
import { State } from '../core/state.js';

export function updateUI(msg = null) {
    if (!msg) {
        msg = State.getLastGameState();
    }
    // Update main screen
    const joinBtn = document.getElementById('joinOrCreate');
    const nameInput = document.getElementById('name');
    const errorDiv = document.getElementById('mainError');
    const title = document.getElementById('title');

    if (title) title.innerText = t('ui.title');
    if (joinBtn) {
        const params = new URLSearchParams(window.location.search);
        const roomCode = params.get('room');
        joinBtn.innerText = roomCode ? `${t('ui.join')} ${roomCode}` : t('ui.create_new_game');
    }
    if (nameInput) nameInput.placeholder = t('ui.your_name');

    if (errorDiv && !errorDiv.classList.contains('hidden')) {
        const code = errorDiv.dataset.code;
        if (code) {
            errorDiv.innerText = t(`ui.${code}`);
        }
    }

    // Update game screen elements
    const logTitle = document.getElementById('logTitle');
    if (logTitle) logTitle.innerText = t('ui.log_title');

    const roomCodeLabel = document.getElementById('roomCodeLabel');
    if (roomCodeLabel) roomCodeLabel.innerText = t('ui.room_code_label');

    const copyLinkBtn = document.getElementById('copyLinkBtn');
    if (copyLinkBtn) copyLinkBtn.innerText = t('ui.copy');

    const yourCardsTitle = document.getElementById('yourCardsTitle');
    if (yourCardsTitle) yourCardsTitle.innerText = t('ui.your_cards');

    const revealedCardsTitle = document.getElementById('revealedCardsTitle');
    if (revealedCardsTitle) revealedCardsTitle.innerText = t('ui.revealed_cards');

    const voteTitle = document.getElementById('voteTitle');
    if (voteTitle) voteTitle.innerText = t('ui.vote_hint');

    const exitBtn = document.getElementById('exitBtn');
    if (exitBtn) exitBtn.innerText = t('ui.exit');

    const startBtn = document.getElementById('startBtn');
    if (startBtn) startBtn.innerText = t('ui.ready_to_start');

    const confirmBtn = document.getElementById('confirm');
    if (confirmBtn) confirmBtn.innerText = t('ui.confirm_round_end');

    const waiting = document.getElementById('waiting');
    if (waiting) waiting.innerText = t('ui.waiting_opponents');

    const minPlayersHint = document.getElementById('minPlayersHint');
    if (minPlayersHint && !minPlayersHint.classList.contains('hidden')) {
        minPlayersHint.innerText = t('ui.min_players_to_start', {n: 3});
    }

    if (msg) {
        const phase = msg.phase || 'lobby';
        const phaseEl = document.getElementById('phase');
        if (phaseEl) {
            phaseEl.innerText = t(`ui.phase_${phase}`);
        }

        const roundEl = document.getElementById('round');
        if (roundEl && msg.round) {
            roundEl.innerText = `${t('ui.round')} ${msg.round}`;
        } else if (roundEl) {
            roundEl.innerText = '';
        }

        if (msg.type === 'game_update' || msg.type === 'open_room') {
            const bunker = document.getElementById('bunker');
            const myId = State.getPlayerId();
            const player = msg.players ? msg.players.find(p => p.name === State.getName()) : null; // Note: player might not have ID in msg, but let's see. ReadyHandler sends ready status.
            
            // Actually ReadyHandler sends players with 'ready' property.
            const me = msg.players ? msg.players.find(p => p.name === State.getName()) : null;
            const isReady = me && me.ready;

            if (bunker) {
                if (phase === 'lobby') {
                    if (!isReady) {
                        bunker.innerHTML = `
                            <div class="mb-3 text-center text-sm text-gray-300">${t('ui.waiting_votes')}</div>
                            <button id="readyBtn" class="w-full bg-indigo-600 hover:bg-indigo-500 py-2 rounded font-semibold transition-colors">
                                ${t('ui.ready_to_start')}
                            </button>
                        `;
                        const readyBtn = document.getElementById('readyBtn');
                        if (readyBtn) {
                            readyBtn.onclick = () => {
                                import('../../handler/readyCommand.js').then(m => m.readyCommand());
                            };
                        }
                    } else {
                        bunker.innerText = t('ui.you_voted_start');
                    }
                } else if (phase === 'reveal') {
                    const hasRevealedThisRound = msg.roundReveals && msg.roundReveals[State.getPlayerId()];
                    let eventHtml = '';
                    if (msg.eventIdx !== undefined) {
                        eventHtml = `<div class="mb-3 p-2 bg-gray-800 rounded border-l-2 border-indigo-500 text-xs italic">${t(`bunkers.${msg.eventIdx}.text`)}</div>`;
                    }
                    
                    if (!hasRevealedThisRound) {
                        bunker.innerHTML = `
                             ${eventHtml}
                             <div class="text-center">
                                <div class="text-sm font-medium mb-1">${t('ui.phase_reveal')}</div>
                                <div class="text-xs text-gray-400">${t('ui.choose_card_hint')}</div>
                             </div>
                        `;
                    } else {
                        bunker.innerHTML = `
                            ${eventHtml}
                            <div class="text-center text-sm font-medium text-indigo-400">${t('ui.you_revealed')}</div>
                        `;
                    }
                } else if (phase === 'confirm') {
                        const hasConfirmed = msg.roundConfirms && msg.roundConfirms.includes(State.getPlayerId());
                        let eventHtml = '';
                        if (msg.eventIdx !== undefined) {
                            eventHtml = `<div class="mb-3 p-2 bg-gray-800 rounded border-l-2 border-indigo-500 text-xs italic">${t(`bunkers.${msg.eventIdx}.text`)}</div>`;
                        }

                        if (!hasConfirmed) {
                            bunker.innerHTML = `
                                ${eventHtml}
                                <div class="mb-3 text-center text-sm text-gray-300">${t('ui.waiting_confirms')}</div>
                                <button id="confirmBtn" class="w-full bg-emerald-600 hover:bg-emerald-500 py-2 rounded font-semibold transition-colors">
                                    ${t('ui.confirm_round_end')}
                                </button>
                            `;
                            const confirmBtn = document.getElementById('confirmBtn');
                            if (confirmBtn) {
                                confirmBtn.onclick = () => {
                                    import('../../handler/confirmCommand.js').then(m => m.confirmCommand());
                                };
                            }
                        } else {
                            bunker.innerHTML = `
                                ${eventHtml}
                                <div class="text-center text-sm font-medium text-emerald-400">${t('ui.you_confirmed')}</div>
                            `;
                        }
                    } else {
                        // Game started, show something else or clear
                        if (msg.eventIdx !== undefined) {
                            bunker.innerText = t(`bunkers.${msg.eventIdx}.text`);
                        } else if (msg.bunker) {
                            bunker.innerText = msg.bunker;
                        } else {
                            bunker.innerText = '';
                        }
                    }
                }

            // Update your cards
            if (msg.myCards) {
                const container = document.getElementById('yourCards');
                if (container) {
                    container.innerHTML = '';
                    Object.entries(msg.myCards).forEach(([category, idx]) => {
                        const card = document.createElement('div');
                        const isRevealed = me && me.revealed && me.revealed[category] !== undefined;
                        card.className = `p-3 rounded shadow-sm flex flex-col gap-1 transition-colors ${isRevealed ? 'bg-gray-800 opacity-50 cursor-not-allowed' : 'bg-gray-700 hover:bg-gray-600 cursor-pointer'}`;
                        card.innerHTML = `
                            <div class="text-[10px] uppercase text-gray-400 font-bold tracking-wider">${t(`labels.${category}`)}</div>
                            <div class="text-sm font-medium">${t(`cards.${category}.${idx}.text`)}</div>
                        `;
                        if (phase === 'reveal' && !isRevealed) {
                            card.onclick = () => {
                                import('../../handler/discardCommand.js').then(m => m.discardCommand(category));
                            };
                        }
                        container.appendChild(card);
                    });
                }
            }

            // Update revealed cards
            const board = document.getElementById('revealedBoard');
            if (board && msg.players) {
                board.innerHTML = '';

                if (msg.history && Object.keys(msg.history).length > 0) {
                    // Sort rounds so they appear in descending order (latest first)
                    const rounds = Object.keys(msg.history).sort((a, b) => parseInt(b) - parseInt(a));
                    
                    rounds.forEach(r => {
                        const roundData = msg.history[r];
                        const eventIdx = roundData.eventIdx;
                        const reveals = roundData.reveals;

                        // Create round container
                        const roundContainer = document.createElement('div');
                        roundContainer.className = 'col-span-full mb-6 last:mb-0';
                        
                        // Round Header with Event
                        const eventSection = document.createElement('div');
                        eventSection.className = 'bg-gray-800 border-l-4 border-indigo-500 p-3 rounded mb-3 shadow-md';
                        eventSection.innerHTML = `
                            <div class="flex justify-between items-center mb-1">
                                <div class="text-[10px] uppercase text-indigo-400 font-bold">${t('ui.round')} ${r} - ${t('ui.bunker_event')}</div>
                            </div>
                            <div class="text-sm italic">${t(`bunkers.${eventIdx}.text`)}</div>
                        `;
                        roundContainer.appendChild(eventSection);

                        // Grid for players' cards in this round
                        const revealGrid = document.createElement('div');
                        revealGrid.className = 'grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3';
                        
                        Object.entries(reveals).forEach(([pid, category]) => {
                            const p = msg.players.find(player => player.id === pid || player.name === pid);
                            if (!p) return;

                            const cardIdx = p.revealed ? p.revealed[category] : undefined;
                            if (cardIdx === undefined) return;

                            const pSection = document.createElement('div');
                            pSection.className = 'bg-gray-700/50 p-3 rounded shadow-sm border border-gray-700';
                            pSection.innerHTML = `
                                <div class="text-xs font-bold mb-1 text-gray-200 border-b border-gray-600 pb-1">${p.name}</div>
                                <div class="text-[10px] uppercase text-gray-500 font-bold tracking-wider mb-0.5">${t(`labels.${category}`)}</div>
                                <div class="text-xs">${t(`cards.${category}.${cardIdx}.text`)}</div>
                            `;
                            revealGrid.appendChild(pSection);
                        });
                        
                        roundContainer.appendChild(revealGrid);
                        board.appendChild(roundContainer);
                    });
                }
            }
        }
    }
}

export function showError(code, message) {
    const errorDiv = document.getElementById('mainError');
    if (errorDiv) {
        errorDiv.innerText = t(`ui.${code}`) || message || code;
        errorDiv.dataset.code = code;
        errorDiv.classList.remove('hidden');
        errorDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

export function updateConnectionStatus(connected) {
    const el = document.getElementById('connectionStatus');
    if (el) {
        if (connected) {
            el.classList.remove('bg-orange-500');
            el.classList.add('bg-green-500');
        } else {
            el.classList.remove('bg-green-500');
            el.classList.add('bg-orange-500');
        }
    }
}

export function showLoader(textKey = 'ui.connecting') {
    const loader = document.getElementById('loader');
    const loaderText = document.getElementById('loaderText');
    if (loader) {
        if (loaderText) loaderText.innerText = t(textKey);
        loader.classList.remove('hidden');
    }
}

export function hideLoader() {
    const loader = document.getElementById('loader');
    if (loader) {
        loader.classList.add('hidden');
    }
}
