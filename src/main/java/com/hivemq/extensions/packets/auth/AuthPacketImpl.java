package com.hivemq.extensions.packets.auth;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.AUTHPacket;
import com.hivemq.mqtt.message.auth.AUTH;

import java.nio.ByteBuffer;

public class AuthPacketImpl implements AUTHPacket {
    private final ByteBuffer data;
    private final String method;
    private final int reasonCode;
    private final String reasonString;

    public AuthPacketImpl(@NotNull final AUTH auth) {
        this.data = ByteBuffer.wrap(auth.getAuthData());
        this.method = auth.getAuthMethod();
        this.reasonCode = auth.getReasonCode().getCode();
        this.reasonString = auth.getReasonString();
    }

    @Override
    public @com.hivemq.extension.sdk.api.annotations.NotNull ByteBuffer getAuthenticationData() {
        return data;
    }

    @Override
    public @com.hivemq.extension.sdk.api.annotations.NotNull String getAuthenticationMethod() {
        return method;
    }

    @Override
    public @Nullable String getReasonString() {
        return reasonString;
    }

    @Override
    public @com.hivemq.extension.sdk.api.annotations.NotNull int getReasonCode() {
        return reasonCode;
    }
}