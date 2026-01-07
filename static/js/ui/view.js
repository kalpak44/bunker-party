import { t } from '../core/i18n.js';

export function updateUI() {
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
