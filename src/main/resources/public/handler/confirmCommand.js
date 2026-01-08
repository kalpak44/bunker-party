import { socket } from '../js/api/socket.js';
import { State } from '../js/core/state.js';

export function confirmCommand() {
    const roomId = State.getRoom();
    const playerId = State.getPlayerId();
    
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: "confirm",
            roomId: roomId,
            playerId: playerId
        }));
    }
}
