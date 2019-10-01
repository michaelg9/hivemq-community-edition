package com.hivemq.extensions.handler;

import com.hivemq.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingReqInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingReqInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingRespOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingRespOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingRespOutboundOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 */
public class PingInterceptorHandlerTest {

    private PluginTaskExecutor executor1;
    private EmbeddedChannel channel;

    public static AtomicBoolean isTriggered = new AtomicBoolean();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private PluginOutPutAsyncer asyncer;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private PluginTaskExecutorService pluginTaskExecutorService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        isTriggered.set(false);
        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        when(plugin.getId()).thenReturn("plugin");

        asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1);

        final PingInterceptorHandler handler =
                new PingInterceptorHandler(pluginTaskExecutorService, asyncer, hiveMQExtensions);
        channel.pipeline().addLast(handler);
    }

    @After
    public void tearDown() {
        executor1.stop();
        channel.close();
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_pingreq_channel_closed() {
        channel.close();
        channel.writeInbound(new PINGREQ());
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_pingresp_channel_closed() {
        channel.close();
        channel.writeOutbound(new PINGRESP());
    }

    @Test(timeout = 5000)
    public void test_read_simple_pingreq() throws Exception {
        final ClientContextImpl clientContext
                = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final PingReqInboundInterceptor interceptor = getIsolatedInboundInterceptor("SimplePingReqTestInterceptor");
        clientContext.addPingRequestInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new PINGREQ());
        PINGREQ pingreq = channel.readInbound();
        while (pingreq == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingreq = channel.readInbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertNotNull(pingreq);
        isTriggered.set(false);

    }

    @Test(timeout = 5000)
    public void test_read_advanced_pingreq() throws Exception {
        final ClientContextImpl clientContext
                = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final PingReqInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("AdvancedPingReqTestInterceptor");
        clientContext.addPingRequestInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new PINGREQ());
        PINGREQ pingreq = channel.readInbound();
        while (pingreq == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingreq = channel.readInbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertNotNull(pingreq);
        isTriggered.set(false);
    }

    @Test(timeout = 5000)
    public void test_read_simple_pingresp() throws Exception {
        final ClientContextImpl clientContext
                = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final PingRespOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("SimplePingRespTestInterceptor");
        clientContext.addPingResponseOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeOutbound(new PINGRESP());
        PINGRESP pingresp = channel.readOutbound();
        while (pingresp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingresp = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertNotNull(pingresp);
        isTriggered.set(false);
    }

    @Test(timeout = 40000)
    public void test_read_advanced_pingresp() throws Exception {
        final ClientContextImpl clientContext
                = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final PingRespOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("AdvancedPingRespTestInterceptor");
        clientContext.addPingResponseOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeOutbound(new PINGRESP());
        PINGRESP pingresp = channel.readOutbound();
        while (pingresp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingresp = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertNotNull(pingresp);
        isTriggered.set(false);
    }


    private PingReqInboundInterceptor getIsolatedInboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PingInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PingInterceptorHandlerTest$" + name);

        final PingReqInboundInterceptor interceptor =
                (PingReqInboundInterceptor) interceptorClass.newInstance();

        return interceptor;
    }

    private PingRespOutboundInterceptor getIsolatedOutboundInterceptor(final @NotNull String name)
            throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PingInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PingInterceptorHandlerTest$" + name);

        final PingRespOutboundInterceptor interceptor =
                (PingRespOutboundInterceptor) interceptorClass.newInstance();

        return interceptor;
    }

    public static class SimplePingReqTestInterceptor implements PingReqInboundInterceptor {

        @Override
        public void onInboundPingReq(
                final @NotNull PingReqInboundInput pingReqInboundInput,
                final @NotNull PingReqInboundOutput pingReqInboundOutput) {
            System.out.println("Intercepting PINGREQ at " + System.currentTimeMillis());
            isTriggered.set(true);
        }

    }

    public static class SimplePingRespTestInterceptor implements PingRespOutboundInterceptor {

        @Override
        public void onOutboundPingResp(
                final @NotNull PingRespOutboundInput pingRespOutboundInput,
                final @NotNull PingRespOutboundOutput pingRespOutboundOutput) {
            System.out.println("Intercepting PINGRESP at " + System.currentTimeMillis());
            isTriggered.set(true);
        }
    }

    public static class AdvancedPingReqTestInterceptor implements PingReqInboundInterceptor {

        @Override
        public void onInboundPingReq(
                final @NotNull PingReqInboundInput pingReqInboundInput,
                final @NotNull PingReqInboundOutput pingReqInboundOutput) {
            System.out.println(
                    "Intercepted PINGREQ for client: " + pingReqInboundInput.getClientInformation().getClientId());
            isTriggered.set(true);

        }

    }

    public static class AdvancedPingRespTestInterceptor implements PingRespOutboundInterceptor {

        @Override
        public void onOutboundPingResp(
                final @NotNull PingRespOutboundInput pingRespOutboundInput,
                final @NotNull PingRespOutboundOutput pingRespOutboundOutput) {
            System.out.println("Intercepted PINGRESP for client: " +
                    pingRespOutboundInput.getClientInformation().getClientId());
            isTriggered.set(true);
        }

    }

}