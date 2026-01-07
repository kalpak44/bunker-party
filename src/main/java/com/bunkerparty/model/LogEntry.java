package com.bunkerparty.model;

import java.util.HashMap;
import java.util.Map;

public class LogEntry {
    private final String type;
    private final long timestamp;
    private final Map<String, Object> data;

    public LogEntry(String type, Map<String, Object> data) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>(data);
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    public Object get(String key) {
        return data.get(key);
    }
}
