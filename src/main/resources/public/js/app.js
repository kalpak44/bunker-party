import { State } from './core/state.js';
import { loadTranslations } from './core/i18n.js';
import { updateUI, showLoader, showError } from './ui/view.js';
import { t } from './core/i18n.js';
import { connect, socket } from './api/socket.js';
import { handleNewGameClick } from '../handler/newGameCommand.js';
import { joinGameCommand } from '../handler/joinGameCommand.js';

export async function setLanguage(lang) {
    State.setLang(lang);
    await loadTranslations(lang);
    
    const mainLang = document.getElementById('mainLang');
    const gameLang = document.getElementById('lang');
    if (mainLang) mainLang.value = lang;
    if (gameLang) gameLang.value = lang;
    
    updateUI();
}

export function cleanupSession() {
    State.setName('');
    State.setToken('');
    State.setRoom('');
    State.setLastGameState(null);
    const nameInput = document.getElementById('name');
    if (nameInput) nameInput.value = '';
    
    const url = new URL(window.location.href);
    url.searchParams.delete('room');
    window.history.pushState({}, '', url.toString());

    const gameEl = document.getElementById('game');
    const mainEl = document.getElementById('main');
    if (gameEl) gameEl.classList.add('hidden');
    if (mainEl) mainEl.classList.remove('hidden');

    updateUI();
}

export function init() {
    connect();

    const params = new URLSearchParams(window.location.search);
    const roomCode = params.get('room');

    const lastRoom = State.getRoom();
    if (roomCode && lastRoom && roomCode !== lastRoom) {
        console.log('Detected room change, cleaning up session');
        // We wait for socket to be open to send leave message if needed
        const checkLeave = setInterval(() => {
            if (socket && socket.readyState === WebSocket.OPEN) {
                clearInterval(checkLeave);
                socket.send(JSON.stringify({type: "leave_game", roomId: lastRoom}));
            }
        }, 100);
        // Timeout to not wait forever if socket is broken
        setTimeout(() => clearInterval(checkLeave), 2000);
        
        cleanupSession();
    }

    const savedName = State.getName();
    const nameInput = document.getElementById('name');
    if (nameInput && savedName) {
        nameInput.value = savedName;
    }

    const savedLang = State.getLang();
    setLanguage(savedLang);

    const mainLang = document.getElementById('mainLang');
    if (mainLang) {
        mainLang.addEventListener('change', () => setLanguage(mainLang.value));
    }

    const gameLang = document.getElementById('lang');
    if (gameLang) {
        gameLang.addEventListener('change', () => setLanguage(gameLang.value));
    }

    const exitBtn = document.getElementById('exitBtn');
    if (exitBtn) {
        exitBtn.addEventListener('click', () => {
            const currentRoom = State.getRoom();
            if (currentRoom && socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({type: "leave_game", roomId: currentRoom}));
            }
            cleanupSession();
        });
    }

    const joinBtn = document.getElementById('joinOrCreate');
    if (joinBtn) {
        joinBtn.addEventListener('click', () => {
            const name = document.getElementById('name')?.value.trim();
            if (!name) {
                showError('name_required');
                return;
            }
            if (name.length > 10) {
                showError('name_too_long');
                return;
            }
            
            State.setName(name);
            showLoader('ui.joining');

            if (roomCode) {
                joinGameCommand(name, roomCode);
            } else {
                handleNewGameClick(name);
            }
        });
    }

    const copyBtn = document.getElementById('copyLinkBtn');
    if (copyBtn) {
        copyBtn.addEventListener('click', () => {
            const shareLink = document.getElementById('shareLink');
            if (shareLink) {
                navigator.clipboard.writeText(shareLink.value);
            }
        });
    }

    // Auto-join if roomCode and savedName are present
    if (roomCode && savedName) {
        showLoader('ui.joining');
    }
}
