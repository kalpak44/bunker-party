// Minimal global state - only connection data
export const state = {
    ws: null,
    myName: "",
    myLang: "en",
    currentRoomId: "",
    reconnectAttempts: 0,
    MAX_RECONNECT_ATTEMPTS: 3,
    // Response handlers for request/response pattern
    pendingRequests: new Map(),
    // Track displayed logs to avoid duplicates
    displayedLogCount: 0
};
