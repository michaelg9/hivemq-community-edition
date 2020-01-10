/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.packets.auth;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.AUTHPacket;
import com.hivemq.mqtt.message.auth.AUTH;

import java.nio.ByteBuffer;
/**
 * @author Michael Michaelides
 * @since 4.2.0
 */
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