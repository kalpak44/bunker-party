import { t } from '../js/core/i18n.js';

export function roomOpened(msg) {
    console.log('Received open_room message:', msg);
    
    const mainScreen = document.getElementById('main');
    const gameScreen = document.getElementById('game');
    const roomCode = document.getElementById('roomCode');
    const shareLink = document.getElementById('shareLink');

    if (mainScreen) mainScreen.classList.add('hidden');
    if (gameScreen) gameScreen.classList.remove('hidden');

    if (msg.room_id) {
        if (roomCode) roomCode.innerText = msg.room_id;

        const url = new URL(window.location.href);
        url.searchParams.set('room', msg.room_id);
        const fullUrl = url.toString();
        
        if (shareLink) {
            shareLink.value = fullUrl;
        }

        const qrcodeContainer = document.getElementById('qrcode');
        if (qrcodeContainer) {
            qrcodeContainer.innerHTML = '';
            new QRCode(qrcodeContainer, {
                text: fullUrl,
                width: 128,
                height: 128,
                colorDark : "#000000",
                colorLight : "#ffffff",
                correctLevel : QRCode.CorrectLevel.H
            });
        }
        
        // Update URL without reloading
        window.history.pushState({path: fullUrl}, '', fullUrl);
    }

    if (msg.players) {
        updatePlayersList(msg.players);
    }
}

export function updatePlayersList(players) {
    const playersContainer = document.getElementById('players');
    if (playersContainer) {
        playersContainer.innerHTML = '';
        players.forEach(p => {
            const pEl = document.createElement('div');
            pEl.className = 'flex items-center gap-2 py-1 border-b border-gray-700 last:border-0';
            pEl.innerHTML = `
                <div class="w-2 h-2 rounded-full ${p.online ? 'bg-green-500' : 'bg-gray-500'}"></div>
                <span>${p.name}</span>
            `;
            playersContainer.appendChild(pEl);
        });
    }

    const minPlayersHint = document.getElementById('minPlayersHint');
    if (minPlayersHint) {
        if (players.length < 3) {
            minPlayersHint.innerText = t('ui.min_players_to_start', {n: 3});
            minPlayersHint.classList.remove('hidden');
        } else {
            minPlayersHint.classList.add('hidden');
        }
    }
}
