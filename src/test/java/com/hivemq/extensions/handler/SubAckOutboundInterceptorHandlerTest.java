package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.suback.SubAckOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubAckOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubAckOutboundOutput;
import com.hivemq.extension.sdk.api.packets.suback.ModifiableSubAckPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
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
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubAckOutboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtension extension;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private ClientContextImpl clientContext;

    @Mock
    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor;
    private EmbeddedChannel channel;
    public static AtomicBoolean isTriggered = new AtomicBoolean();

    @Before
    public void setup() {
        initMocks(this);
        isTriggered.set(false);
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        when(extension.getId()).thenReturn("extension");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor);

        final SubAckOutboundInterceptorHandler handler = new SubAckOutboundInterceptorHandler(
                configurationService, asyncer, hiveMQExtensions, pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test
    public void test_intercept_simple_subAck() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final SubAckOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("SimpleSubAckTestInterceptor");
        clientContext.addSubAckOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertNotNull(subAck);
    }

    @Test
    public void test_modify_subAck() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final SubAckOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("TestModifySubAckInterceptor");
        clientContext.addSubAckOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertEquals(
                SubackReasonCode.GRANTED_QOS_1, SubackReasonCode.valueOf(subAck.getReasonCodes().get(0).name()));
        Assert.assertNotNull(subAck);
    }

    @Test
    public void test_outbound_exception() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final SubAckOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("TestExceptionSubAckInterceptor");
        clientContext.addSubAckOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());

    }

    private @NotNull SUBACK testSubAck() {
        return new SUBACK(
                1, ImmutableList.of(Mqtt5SubAckReasonCode.GRANTED_QOS_0), "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    private SubAckOutboundInterceptor getIsolatedOutboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.SubAckOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.SubAckOutboundInterceptorHandlerTest$" + name);

        return (SubAckOutboundInterceptor) interceptorClass.newInstance();
    }

    public static class SimpleSubAckTestInterceptor implements SubAckOutboundInterceptor {

        @Override
        public void onOutboundSubAck(
                final @NotNull SubAckOutboundInput subAckOutboundInput,
                final @NotNull SubAckOutboundOutput subAckOutboundOutput) {
            System.out.println("Intercepting SUBACK at: " + System.currentTimeMillis());
            isTriggered.set(true);
        }
    }

    public static class TestModifySubAckInterceptor implements SubAckOutboundInterceptor {

        @Override
        public void onOutboundSubAck(
                final @NotNull SubAckOutboundInput subAckOutboundInput,
                final @NotNull SubAckOutboundOutput subAckOutboundOutput) {
            final ModifiableSubAckPacket packet = subAckOutboundOutput.getSubAckPacket();
            final ArrayList<SubackReasonCode> subAckReasonCodes = new ArrayList<>();
            subAckReasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
            packet.setReasonCodes(subAckReasonCodes);
            isTriggered.set(true);
        }
    }

    public static class TestExceptionSubAckInterceptor implements SubAckOutboundInterceptor {

        @Override
        public void onOutboundSubAck(
                final @NotNull SubAckOutboundInput subAckOutboundInput,
                final @NotNull SubAckOutboundOutput subAckOutboundOutput) {
            isTriggered.set(true);
            final ModifiableSubAckPacket packet = subAckOutboundOutput.getSubAckPacket();
            final ArrayList<SubackReasonCode> subAckReasonCodes = new ArrayList<>();
            subAckReasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
            packet.setReasonCodes(subAckReasonCodes);
            throw new RuntimeException();
        }
    }

}