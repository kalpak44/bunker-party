let currentTranslations = {};

export async function loadTranslations(lang) {
    try {
        const response = await fetch(`/data/${lang}.json`);
        currentTranslations = await response.json();
    } catch (e) {
        console.error('Failed to load translations', e);
    }
}

export function t(key, params = {}) {
    const keys = key.split('.');
    let value = currentTranslations;
    for (const k of keys) {
        value = value?.[k];
    }
    if (typeof value !== 'string') return key;

    Object.keys(params).forEach(p => {
        value = value.replace(`{${p}}`, params[p]);
    });
    return value;
}

export function getCurrentTranslations() {
    return currentTranslations;
}
