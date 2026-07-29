/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor.socket;

public enum SocketMessageType {
    HELLO("hello"),
    CHANGE_REQUEST("change-request"),
    PING("ping"),
    ACCEPTED("accepted"),
    UNTRUSTED("untrusted"),
    CHANGE_RESPONSE("change-response"),
    PONG("pong");

    private final String id;

    private SocketMessageType(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public static SocketMessageType fromId(String id) {
        for (SocketMessageType type : SocketMessageType.values()) {
            if (!type.id.equals(id)) continue;
            return type;
        }
        return null;
    }
}

