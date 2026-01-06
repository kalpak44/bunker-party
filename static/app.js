import { $ } from './utils.js';
import { initializeApp, copyLinkToClipboard, handleJoinOrCreate } from './init.js';
import { sendVoteStart, sendConfirmRoundEnd } from './websocket.js';

// Initialize app on DOM load
window.addEventListener('DOMContentLoaded', initializeApp);

// Event listeners
$("copyLinkBtn").onclick = copyLinkToClipboard;
$("joinOrCreate").onclick = handleJoinOrCreate;
$("startBtn").onclick = sendVoteStart;
$("confirm").onclick = sendConfirmRoundEnd;
