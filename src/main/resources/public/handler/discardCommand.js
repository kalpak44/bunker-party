import { socket } from '../js/api/socket.js';
import { State } from '../js/core/state.js';

export function discardCommand(cardKey) {
    const roomId = State.getRoom();
    const playerId = State.getPlayerId();
    
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: "discard",
            roomId: roomId,
            playerId: playerId,
            cardKey: cardKey
        }));
    }
}
