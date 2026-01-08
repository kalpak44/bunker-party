import {socket} from '../js/api/socket.js';
import { State } from '../js/core/state.js';

export function joinGameCommand(name, roomId) {
    const token = State.getToken();
    socket.send(JSON.stringify({type: "join_game", name, roomId, token}));
}