import {socket} from '../js/api/socket.js';

export function joinGameCommand(name, roomId) {
    socket.send(JSON.stringify({type: "join_game", name, roomId}));
}