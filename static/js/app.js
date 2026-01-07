import { State } from './core/state.js';
import { loadTranslations } from './core/i18n.js';
import { updateUI } from './ui/view.js';
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

export function init() {
    connect();

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

    const joinBtn = document.getElementById('joinOrCreate');
    const params = new URLSearchParams(window.location.search);
    const roomCode = params.get('room');

    if (joinBtn) {
        joinBtn.addEventListener('click', () => {
            const name = document.getElementById('name')?.value.trim();
            if (name) {
                State.setName(name);
            }

            if (roomCode) {
                joinGameCommand(name, roomCode);
            } else {
                handleNewGameClick(name);
            }
        });
    }

    // Auto-join if roomCode and savedName are present
    if (roomCode && savedName) {
        // Wait a bit for socket to connect
        const checkSocket = setInterval(() => {
            if (socket && socket.readyState === WebSocket.OPEN) {
                clearInterval(checkSocket);
                joinGameCommand(savedName, roomCode);
            }
        }, 100);
    }
}
