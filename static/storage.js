// LocalStorage helpers
export function saveSession(name, room, lang) {
    localStorage.setItem('bunker_name', name);
    localStorage.setItem('bunker_room', room);
    localStorage.setItem('bunker_lang', lang);
}

export function getSession() {
    return {
        name: localStorage.getItem('bunker_name') || '',
        room: localStorage.getItem('bunker_room') || '',
        lang: localStorage.getItem('bunker_lang') || 'en'
    };
}

export function clearSession() {
    localStorage.removeItem('bunker_name');
    localStorage.removeItem('bunker_room');
    localStorage.removeItem('bunker_lang');
}
