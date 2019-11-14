package com.hivemq.extension.sdk.api.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;

@FunctionalInterface
public interface ExtendedAuthenticator {
    void onAUTH(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput connectAuthTaskOutput);
}
