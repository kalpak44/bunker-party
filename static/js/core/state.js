export const State = {
    getName: () => localStorage.getItem('name'),
    setName: (name) => localStorage.setItem('name', name),
    getLang: () => localStorage.getItem('lang') || 'en',
    setLang: (lang) => localStorage.setItem('lang', lang),
};
