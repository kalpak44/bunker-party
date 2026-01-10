import { t } from '../js/core/i18n.js';
import { State } from '../js/core/state.js';
import { hideLoader, updateUI } from '../js/ui/view.js';

export function roomOpened(msg) {
    console.log('Received open_room message:', msg);

    if (msg.token) {
        State.setToken(msg.token);
    }

    if (msg.player_id) {
        State.setPlayerId(msg.player_id);
    }

    if (msg.room_id) {
        State.setRoom(msg.room_id);
    }

    hideLoader();
    updateUI(msg);
    
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
}
