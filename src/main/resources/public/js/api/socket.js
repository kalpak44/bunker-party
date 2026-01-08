import { roomOpened, updatePlayersList } from '../../handler/openRoomHandler.js';
import { showError, updateConnectionStatus, hideLoader, updateUI } from '../ui/view.js';
import { cleanupSession } from '../app.js';
import { State } from '../core/state.js';
import { joinGameCommand } from '../../handler/joinGameCommand.js';

function protoWs() {
    return location.protocol === 'https:' ? 'wss' : 'ws';
}

export let socket;
let reconnectTimer;

export function connect() {
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }

    const wsUrl = `${protoWs()}://${location.host}/ws`;
    socket = new WebSocket(wsUrl);

    socket.onopen = () => {
        console.log('Connected to WebSocket');
        updateConnectionStatus(true);

        // Auto-rejoin if we have saved session
        const name = State.getName();
        const room = new URLSearchParams(window.location.search).get('room');
        if (name && room) {
            console.log(`Auto-rejoining room ${room} as ${name}`);
            joinGameCommand(name, room);
        } else {
            hideLoader();
        }
    };

    socket.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        if (msg.type === 'open_room') {
            State.setLastGameState(msg);
            roomOpened(msg);
        } else if (msg.type === 'player_joined') {
            State.setLastGameState(msg);
            if (msg.players) {
                updatePlayersList(msg.players);
            }
            updateUI(msg);
        } else if (msg.type === 'game_update') {
            State.setLastGameState(msg);
            if (msg.players) {
                updatePlayersList(msg.players);
            }
            updateUI(msg);
        } else if (msg.type === 'error') {
            hideLoader();
            if (msg.code === 'room_not_found' || msg.code === 'invalid_token') {
                cleanupSession();
            }
            showError(msg.code, msg.message);
        } else if (msg.type === 'refresh') {
            console.log('Refresh requested');
        }
    };

    socket.onclose = () => {
        console.log('WebSocket connection closed. Retrying in 1 seconds...');
        updateConnectionStatus(false);
        scheduleReconnect();
    };

    socket.onerror = (error) => {
        console.error('WebSocket error:', error);
        socket.close();
    };
}

function scheduleReconnect() {
    if (!reconnectTimer) {
        reconnectTimer = setTimeout(() => {
            console.log('Attempting to reconnect...');
            connect();
        }, 1000);
    }
}
