package com.hivemq.extension.sdk.api.packets.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.nio.ByteBuffer;

public interface AUTHPacket {
    /**
     * The {@link ByteBuffer} contains the data used for the extended authentication.
     * The contents of this data are defined by the authentication method.
     *
     * @return A {@link ByteBuffer} that contains the authentication data.
     */
    @NotNull ByteBuffer getAuthenticationData();

    /**
     * Contains the authentication method that is being used for the extended
     * authentication.
     *
     * @return An {@link String} that contains the authentication method.
     */
    @NotNull String getAuthenticationMethod();

    @Nullable String getReasonString();

    @NotNull int getReasonCode();

}
