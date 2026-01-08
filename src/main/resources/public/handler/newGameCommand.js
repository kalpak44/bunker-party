import {socket} from '../js/api/socket.js';

export function handleNewGameClick(name) {
    socket.send(JSON.stringify({type: "new_game", name}));
}
