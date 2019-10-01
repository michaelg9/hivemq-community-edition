package com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link PingReqInboundInterceptor}
 *
 * @author Robin Atherton
 */
public interface PingReqInboundInput extends ClientBasedInput {

}