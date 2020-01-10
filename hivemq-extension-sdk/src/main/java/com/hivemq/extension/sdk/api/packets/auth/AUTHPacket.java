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
