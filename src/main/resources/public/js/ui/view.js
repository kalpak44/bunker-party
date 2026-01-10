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
    const subtitle = document.getElementById('mainSubtitle');

    if (title) title.innerText = t('ui.title');
    if (subtitle) subtitle.innerText = t('ui.welcome');
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

        if (msg.type === 'game_update' || msg.type === 'open_room' || msg.type === 'player_joined') {
            const bunker = document.getElementById('bunker');
            const playersList = document.getElementById('players');
            const playerCountEl = document.getElementById('playerCount');
            const myId = State.getPlayerId();
            
            // Actually ReadyHandler sends players with 'ready' property.
            const me = msg.players ? msg.players.find(p => p.name === State.getName()) : null;
            const isReady = me && me.ready;

            if (playerCountEl && msg.players) {
                playerCountEl.innerText = msg.players.length;
            }

            if (playersList && msg.players) {
                playersList.innerHTML = '';
                msg.players.forEach(p => {
                    const isMe = p.name === State.getName();
                    const pDiv = document.createElement('div');
                    pDiv.className = `flex items-center justify-between p-3 rounded-xl border transition-all ${
                        p.online ? 'bg-gray-900/40 border-gray-700/50' : 'bg-gray-900/10 border-gray-800 opacity-50'
                    }`;
                    
                    pDiv.innerHTML = `
                        <div class="flex items-center gap-3">
                            <div class="relative">
                                <div class="w-8 h-8 rounded-full bg-gradient-to-br ${isMe ? 'from-blue-500 to-indigo-600' : 'from-gray-600 to-gray-700'} flex items-center justify-center text-xs font-black shadow-inner">
                                    ${p.name.charAt(0).toUpperCase()}
                                </div>
                                <div class="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-gray-800 ${p.online ? 'bg-green-500' : 'bg-gray-500'}"></div>
                            </div>
                            <div>
                                <div class="text-xs font-bold ${isMe ? 'text-blue-400' : 'text-gray-300'}">${p.name}${isMe ? ' <span class="text-[10px] opacity-60">(You)</span>' : ''}</div>
                                <div class="text-[9px] text-gray-500 uppercase tracking-tighter">${p.online ? t('ui.online') : t('ui.offline')}</div>
                            </div>
                        </div>
                        ${p.ready ? `
                            <div class="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[9px] px-2 py-0.5 rounded-full font-black uppercase tracking-widest">Ready</div>
                        ` : `
                            <div class="bg-gray-800 text-gray-600 text-[9px] px-2 py-0.5 rounded-full font-black uppercase tracking-widest italic">Waiting</div>
                        `}
                    `;
                    playersList.appendChild(pDiv);
                });

                const minPlayersHint = document.getElementById('minPlayersHint');
                if (minPlayersHint) {
                    if (msg.players.length < 3) {
                        minPlayersHint.innerText = t('ui.min_players_to_start', {n: 3});
                        minPlayersHint.classList.remove('hidden');
                    } else {
                        minPlayersHint.classList.add('hidden');
                    }
                }
            }

            if (bunker) {
                if (phase === 'lobby') {
                    if (!isReady) {
                        bunker.innerHTML = `
                            <div class="mb-4 text-center">
                                <div class="text-sm font-bold text-gray-300 mb-1 tracking-tight">${t('ui.waiting_votes')}</div>
                                <div class="text-[10px] text-gray-500 uppercase tracking-widest">${t('ui.phase_lobby')}</div>
                            </div>
                            <button id="readyBtn" class="w-full bg-indigo-600 hover:bg-indigo-500 active:scale-[0.98] py-4 rounded-xl font-black text-white shadow-lg shadow-indigo-900/20 transition-all uppercase tracking-widest text-sm">
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
                        bunker.innerHTML = `
                            <div class="py-6 text-center space-y-3">
                                <div class="w-12 h-12 bg-emerald-500/10 border border-emerald-500/20 rounded-full flex items-center justify-center mx-auto">
                                    <svg class="w-6 h-6 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path></svg>
                                </div>
                                <div>
                                    <div class="text-emerald-400 font-black uppercase tracking-widest text-xs">${t('ui.you_voted_start')}</div>
                                    <div class="text-[10px] text-gray-500 mt-1">${t('ui.waiting_opponents')}</div>
                                </div>
                            </div>
                        `;
                    }
                } else if (phase === 'reveal') {
                    const hasRevealedThisRound = msg.roundReveals && msg.roundReveals[State.getPlayerId()];
                    let eventHtml = '';
                    if (msg.eventIdx !== undefined) {
                        eventHtml = `
                            <div class="mb-4 p-4 bg-gray-800 rounded-xl border-l-4 border-indigo-500 shadow-inner">
                                <div class="text-[9px] uppercase tracking-widest text-indigo-400 font-black mb-1">${t('ui.bunker_event')}</div>
                                <div class="text-sm italic leading-relaxed text-gray-200">${t(`bunkers.${msg.eventIdx}.text`)}</div>
                            </div>
                        `;
                    }
                    
                    if (!hasRevealedThisRound) {
                        bunker.innerHTML = `
                             ${eventHtml}
                             <div class="text-center bg-indigo-600/10 border border-indigo-600/20 rounded-xl p-4">
                                <div class="text-sm font-black text-indigo-400 uppercase tracking-widest mb-1">${t('ui.phase_reveal')}</div>
                                <div class="text-xs text-gray-400">${t('ui.choose_card_hint')}</div>
                             </div>
                        `;
                    } else {
                        bunker.innerHTML = `
                            ${eventHtml}
                            <div class="py-4 text-center bg-emerald-600/10 border border-emerald-600/20 rounded-xl">
                                <div class="text-emerald-400 font-black uppercase tracking-widest text-xs">${t('ui.you_revealed')}</div>
                                <div class="text-[10px] text-gray-500 mt-1">${t('ui.waiting_opponents')}</div>
                            </div>
                        `;
                    }
                } else if (phase === 'confirm') {
                        const hasConfirmed = msg.roundConfirms && msg.roundConfirms.includes(State.getPlayerId());
                        let eventHtml = '';
                        if (msg.eventIdx !== undefined) {
                            eventHtml = `
                                <div class="mb-4 p-4 bg-gray-800 rounded-xl border-l-4 border-indigo-500 shadow-inner">
                                    <div class="text-[9px] uppercase tracking-widest text-indigo-400 font-black mb-1">${t('ui.bunker_event')}</div>
                                    <div class="text-sm italic leading-relaxed text-gray-200">${t(`bunkers.${msg.eventIdx}.text`)}</div>
                                </div>
                            `;
                        }

                        if (!hasConfirmed) {
                            bunker.innerHTML = `
                                ${eventHtml}
                                <div class="mb-4 text-center">
                                    <div class="text-sm font-bold text-gray-300 mb-1 tracking-tight">${t('ui.waiting_confirms')}</div>
                                    <div class="text-[10px] text-gray-500 uppercase tracking-widest">${t('ui.phase_confirm')}</div>
                                </div>
                                <button id="confirmBtn" class="w-full bg-emerald-600 hover:bg-emerald-500 active:scale-[0.98] py-4 rounded-xl font-black text-white shadow-lg shadow-emerald-900/20 transition-all uppercase tracking-widest text-sm">
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
                                <div class="py-4 text-center bg-emerald-600/10 border border-emerald-600/20 rounded-xl">
                                    <div class="text-emerald-400 font-black uppercase tracking-widest text-xs">${t('ui.you_confirmed')}</div>
                                    <div class="text-[10px] text-gray-500 mt-1">${t('ui.waiting_opponents')}</div>
                                </div>
                            `;
                        }
                    } else if (phase === 'game_over') {
                        bunker.innerHTML = `
                            <div class="text-center py-6">
                                <div class="text-4xl mb-4 animate-bounce">🏆</div>
                                <div class="text-xl font-black text-indigo-400 mb-2 uppercase tracking-tighter">${t('ui.game_over')}</div>
                                <div class="text-sm text-gray-400 mb-6 font-medium">${t('ui.all_cards_used')}</div>
                                <button id="newGameBtn" class="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 active:scale-[0.98] py-4 rounded-xl font-black text-white shadow-xl shadow-indigo-900/20 transition-all uppercase tracking-widest">
                                    ${t('ui.create_new_game')}
                                </button>
                            </div>
                        `;
                        const newGameBtn = document.getElementById('newGameBtn');
                        if (newGameBtn) {
                            newGameBtn.onclick = () => {
                                window.location.href = '/';
                            };
                        }
                    } else {
                        // Game started, show something else or clear
                        if (msg.eventIdx !== undefined) {
                            bunker.innerHTML = `
                                <div class="p-4 bg-gray-800 rounded-xl border-l-4 border-indigo-500 shadow-inner">
                                    <div class="text-[9px] uppercase tracking-widest text-indigo-400 font-black mb-1">${t('ui.bunker_event')}</div>
                                    <div class="text-sm italic leading-relaxed text-gray-200">${t(`bunkers.${msg.eventIdx}.text`)}</div>
                                </div>
                            `;
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
                        card.className = `group p-4 rounded-xl border-2 transition-all duration-300 ${
                            isRevealed 
                                ? 'bg-gray-900/50 border-gray-800 opacity-40 cursor-not-allowed grayscale' 
                                : 'bg-gray-800 border-gray-700 hover:border-blue-500 hover:shadow-lg hover:shadow-blue-900/10 cursor-pointer active:scale-[0.98]'
                        }`;
                        card.innerHTML = `
                            <div class="flex items-center justify-between mb-2">
                                <div class="text-[9px] uppercase text-gray-500 font-black tracking-widest group-hover:text-blue-400 transition-colors">${t(`labels.${category}`)}</div>
                                ${isRevealed ? '<div class="w-2 h-2 rounded-full bg-gray-700"></div>' : '<div class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></div>'}
                            </div>
                            <div class="text-sm font-bold text-gray-200 leading-snug">${t(`cards.${category}.${idx}.text`)}</div>
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
                        roundContainer.className = 'col-span-full space-y-4 mb-8 last:mb-0';
                        
                        // Round Header with Event
                        const eventSection = document.createElement('div');
                        eventSection.className = 'bg-gray-800/80 border-l-4 border-emerald-500 p-4 rounded-xl shadow-lg border border-gray-700/50 backdrop-blur-sm';
                        eventSection.innerHTML = `
                            <div class="flex justify-between items-center mb-2">
                                <div class="text-[10px] uppercase text-emerald-400 font-black tracking-[0.2em]">${t('ui.round')} ${r}</div>
                                <div class="text-[10px] uppercase text-gray-500 font-bold">${t('ui.bunker_event')}</div>
                            </div>
                            <div class="text-sm italic text-gray-200 leading-relaxed">${t(`bunkers.${eventIdx}.text`)}</div>
                        `;
                        roundContainer.appendChild(eventSection);

                        // Grid for players' cards in this round
                        const revealGrid = document.createElement('div');
                        revealGrid.className = 'grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4';
                        
                        Object.entries(reveals).forEach(([pid, category]) => {
                            const p = msg.players.find(player => player.id === pid || player.name === pid);
                            if (!p) return;

                            const cardIdx = p.revealed ? p.revealed[category] : undefined;
                            if (cardIdx === undefined) return;

                            const pSection = document.createElement('div');
                            pSection.className = 'bg-gray-800 p-4 rounded-xl border border-gray-700 shadow-sm hover:border-gray-600 transition-colors';
                            pSection.innerHTML = `
                                <div class="flex items-center gap-2 mb-3 border-b border-gray-700/50 pb-2">
                                    <div class="w-6 h-6 rounded-full bg-gray-700 flex items-center justify-center text-[10px] font-black uppercase">${p.name.charAt(0)}</div>
                                    <div class="text-xs font-black text-gray-300 uppercase tracking-tight">${p.name}</div>
                                </div>
                                <div class="text-[9px] uppercase text-blue-500 font-black tracking-widest mb-1">${t(`labels.${category}`)}</div>
                                <div class="text-xs font-medium text-gray-200 leading-normal">${t(`cards.${category}.${cardIdx}.text`)}</div>
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
