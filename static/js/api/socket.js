import { roomOpened, updatePlayersList } from '../../handler/openRoomHandler.js';
import { showError, updateConnectionStatus } from '../ui/view.js';

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
    };

    socket.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        if (msg.type === 'open_room') {
            roomOpened(msg);
        } else if (msg.type === 'player_joined') {
            if (msg.players) {
                updatePlayersList(msg.players);
            }
        } else if (msg.type === 'error') {
            if (msg.code === 'room_not_found') {
                const url = new URL(window.location.href);
                url.searchParams.delete('room');
                window.history.pushState({path: url.toString()}, '', url.toString());
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
