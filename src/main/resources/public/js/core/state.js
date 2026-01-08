export const State = {
    getName: () => localStorage.getItem('name'),
    setName: (name) => localStorage.setItem('name', name),
    getToken: () => localStorage.getItem('token'),
    setToken: (token) => localStorage.setItem('token', token),
    getRoom: () => localStorage.getItem('room'),
    setRoom: (room) => localStorage.setItem('room', room),
    getLang: () => localStorage.getItem('lang') || 'en',
    setLang: (lang) => localStorage.setItem('lang', lang),
    getPlayerId: () => localStorage.getItem('playerId'),
    setPlayerId: (id) => localStorage.setItem('playerId', id),
    getLastGameState: () => JSON.parse(sessionStorage.getItem('lastGameState') || 'null'),
    setLastGameState: (state) => sessionStorage.setItem('lastGameState', JSON.stringify(state)),
};
