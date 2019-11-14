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

package com.hivemq.mqtt.handler.auth;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;
import com.hivemq.extensions.client.parameter.AuthenticatorProviderInputFactory;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.services.auth.*;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.function.Supplier;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.AUTH_IN_PROGRESS_MESSAGE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_DECODER;
import static com.hivemq.configuration.service.InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS;
import static com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION;
import static com.hivemq.util.ChannelAttributes.AUTH_TASK_CONTEXT;

/**
 * @author Lukas Brandl
 */
@Singleton
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<AUTH> {
    private final @NotNull Authenticators authenticators;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull MqttConnacker mqttConnacker;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull AuthenticatorProviderInputFactory authenticatorProviderInputFactory;

    @Inject
    public AuthHandler(
            final @NotNull Authenticators authenticators,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull FullConfigurationService configurationService,
            final @NotNull AuthenticatorProviderInputFactory authenticatorProviderInputFactory) {
        this.authenticators = authenticators;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.mqttConnacker = mqttConnacker;
        this.asyncer = asyncer;
        this.configurationService = configurationService;
        this.authenticatorProviderInputFactory = authenticatorProviderInputFactory;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final AUTH msg) throws Exception {
        final Map<String, WrappedAuthenticatorProvider> authenticatorProviderMap = authenticators.getAuthenticatorProviderMap();
        final String authMethod = msg.getAuthMethod();
        assert authMethod.equals(ctx.channel().attr(ChannelAttributes.AUTH_METHOD).get());
        final ConnectAuthTaskContext context = ctx.channel().attr(AUTH_TASK_CONTEXT).get();
        final CONNECT connect = context.getConnect();

        final ConnectAuthTaskInput input = new ConnectAuthTaskInput(connect, ctx);
        input.setAuth(msg);
        final AuthenticatorProviderInput
                authenticatorProviderInput = authenticatorProviderInputFactory.createInput(ctx, connect.getClientIdentifier());

        for (final WrappedAuthenticatorProvider wrapped : authenticatorProviderMap.values()) {

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    context, input, context, new SimpleAuthTask(wrapped, authenticatorProviderInput));
        }
    }

}
