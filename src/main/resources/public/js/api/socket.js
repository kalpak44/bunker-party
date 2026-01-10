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
let heartbeatTimer;
let reconnectDelay = 1000;
const MAX_RECONNECT_DELAY = 30000;
const HEARTBEAT_INTERVAL = 30000;

export function connect() {
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }

    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
        return;
    }

    const wsUrl = `${protoWs()}://${location.host}/ws`;
    socket = new WebSocket(wsUrl);

    socket.onopen = () => {
        console.log('Connected to WebSocket');
        updateConnectionStatus(true);
        reconnectDelay = 1000; // Reset delay on successful connection
        startHeartbeat();

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
        } else if (msg.type === 'pong') {
            // Heartbeat received
        }
    };

    socket.onclose = () => {
        console.log(`WebSocket connection closed. Retrying in ${reconnectDelay / 1000} seconds...`);
        updateConnectionStatus(false);
        stopHeartbeat();
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
            reconnectTimer = null;
            connect();
            // Increase delay for next attempt
            reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY);
        }, reconnectDelay);
    }
}

function startHeartbeat() {
    stopHeartbeat();
    heartbeatTimer = setInterval(() => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({type: 'ping'}));
        }
    }, HEARTBEAT_INTERVAL);
}

function stopHeartbeat() {
    if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
    }
}

// Reconnect immediately when page becomes visible or network is back
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        if (!socket || socket.readyState === WebSocket.CLOSED) {
            console.log('Page visible, attempting immediate reconnect...');
            reconnectDelay = 1000; // Reset delay
            connect();
        }
    }
});

window.addEventListener('online', () => {
    if (!socket || socket.readyState === WebSocket.CLOSED) {
        console.log('Network online, attempting immediate reconnect...');
        reconnectDelay = 1000; // Reset delay
        connect();
    }
});
