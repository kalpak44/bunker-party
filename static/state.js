// Global state management
export const state = {
    ws: null,
    ui: {},
    labels: {},
    character: {},
    gameState: {},
    myName: "",
    myLang: "en",
    myNameCanonical: "",
    hasStarted: false,
    revealedThisRound: false,
    currentRoomId: "",
    reconnectAttempts: 0,
    MAX_RECONNECT_ATTEMPTS: 3
};
