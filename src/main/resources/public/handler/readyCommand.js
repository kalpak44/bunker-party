import { socket } from '../js/api/socket.js';
import { State } from '../js/core/state.js';

export function readyCommand() {
    const roomId = State.getRoom();
    const playerId = State.getPlayerId();
    
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: "ready",
            roomId: roomId,
            playerId: playerId
        }));
    }
}
