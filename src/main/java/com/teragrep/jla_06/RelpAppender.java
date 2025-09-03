/*
   Log4j2 RELP Plugin
   Copyright (C) 2021  Suomen Kanuuna Oy

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.teragrep.jla_06;

import com.teragrep.jla_06.lib.RelpLogAppender;
import com.teragrep.jla_06.lib.RelpLogAppenderImpl;
import com.teragrep.jla_06.lib.RelpLogAppenderSynchronized;
import com.teragrep.jla_06.lib.syslog.SyslogRecord;
import com.teragrep.jla_06.lib.syslog.SyslogRecordConfigured;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithEventID;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithOrigin;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithPayload;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithSystemID;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithTimestamp;
import com.teragrep.jla_06.lib.syslog.hostname.Hostname;
import com.teragrep.rlp_01.client.IManagedRelpConnection;
import com.teragrep.rlp_01.client.ManagedRelpConnectionStub;
import com.teragrep.rlp_01.client.RelpConfig;
import com.teragrep.rlp_01.client.RelpConnectionFactory;
import com.teragrep.rlp_01.client.SSLContextSupplier;
import com.teragrep.rlp_01.client.SSLContextSupplierKeystore;
import com.teragrep.rlp_01.client.SSLContextSupplierStub;
import com.teragrep.rlp_01.client.SocketConfig;
import com.teragrep.rlp_01.client.SocketConfigImpl;
import com.teragrep.rlp_01.pool.Pool;
import com.teragrep.rlp_01.pool.UnboundPool;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Plugin(
        name = "RelpAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true
)
public class RelpAppender extends AbstractAppender {

    private final RelpLogAppender relpLogAppender;
    private final SyslogRecordFactory syslogRecordFactory;

    protected RelpAppender(
            String name,
            Filter filter,
            Layout<? extends Serializable> layout,
            boolean ignoreExceptions,
            Property[] properties,
            String hostname,
            String appName,
            int readTimeout,
            int writeTimeout,
            int reconnectInterval,
            int connectionTimeout,
            boolean useSD,
            String relpHostAddress,
            int relpPort,
            boolean useTLS,
            String keystorePath,
            String keystorePassword,
            String tlsProtocol,
            boolean enableSystemId,
            String systemID,
            int rebindRequestAmount,
            boolean rebindEnabled,
            int reconnectIfNoMessagesInterval,
            boolean synchronizedAccess
    ) {
        super(name, filter, layout, ignoreExceptions, properties);
        final SSLContextSupplier sslContextSupplier;
        if (useTLS) {
            sslContextSupplier = new SSLContextSupplierKeystore(keystorePath, keystorePassword, tlsProtocol);
        }
        else {
            sslContextSupplier = new SSLContextSupplierStub();
        }
        String originalHostname = new Hostname("").hostname();
        this.syslogRecordFactory = new SyslogRecordFactory(
                hostname,
                appName,
                originalHostname,
                useSD,
                enableSystemId,
                systemID
        );
        boolean maxIdleEnabled = (reconnectIfNoMessagesInterval > 0);

        final RelpConfig relpConfig = new RelpConfig(
                relpHostAddress,
                relpPort,
                reconnectInterval,
                rebindRequestAmount,
                rebindEnabled,
                Duration.ofMillis(reconnectIfNoMessagesInterval),
                maxIdleEnabled
        );
        final SocketConfig socketConfig = new SocketConfigImpl(readTimeout, writeTimeout, connectionTimeout, false);
        RelpConnectionFactory relpConnectionFactory = new RelpConnectionFactory(
                relpConfig,
                socketConfig,
                sslContextSupplier
        );

        Pool<IManagedRelpConnection> relpConnectionPool = new UnboundPool<>(
                relpConnectionFactory,
                new ManagedRelpConnectionStub()
        );
        if (!synchronizedAccess) {
            this.relpLogAppender = new RelpLogAppenderImpl(relpConnectionPool);
        }
        else {
            this.relpLogAppender = new RelpLogAppenderSynchronized(new RelpLogAppenderImpl(relpConnectionPool));
        }
    }

    private static class SyslogRecordFactory {

        private final String hostname;
        private final String appName;
        private final String originalHostname;
        private final boolean useSD;
        private final boolean enableSystemID;
        private final String systemID;

        public SyslogRecordFactory(
                String hostname,
                String appName,
                String originalHostname,
                boolean useSD,
                boolean enableSystemID,
                String systemID
        ) {
            this.hostname = hostname;
            this.appName = appName;
            this.originalHostname = originalHostname;
            this.useSD = useSD;
            this.enableSystemID = enableSystemID;
            this.systemID = systemID;
        }

        public SyslogRecord create(String payload) {
            SyslogRecord syslogRecord = new SyslogRecordConfigured(hostname, appName);
            syslogRecord = new SyslogRecordWithTimestamp(syslogRecord);

            // Add SD if enabled
            if (this.useSD) {
                syslogRecord = new SyslogRecordWithOrigin(syslogRecord, originalHostname);
                syslogRecord = new SyslogRecordWithEventID(syslogRecord, originalHostname);
            }
            if (enableSystemID) {
                syslogRecord = new SyslogRecordWithSystemID(syslogRecord, systemID);
            }

            return new SyslogRecordWithPayload(syslogRecord, payload);
        }
    }

    @Override
    public void append(LogEvent event) {
        SyslogRecord syslogRecord = syslogRecordFactory
                .create(new String(getLayout().toByteArray(event), StandardCharsets.UTF_8));
        relpLogAppender.append(syslogRecord);
    }

    @PluginFactory
    public static RelpAppender createAppender(
            @PluginAttribute(
                    value = "name",
                    defaultString = "localhost"
            ) String name,
            @PluginAttribute(
                    value = "ignoreExceptions",
                    defaultBoolean = false
            ) boolean ignoreExceptions,
            @PluginAttribute(
                    value = "hostname",
                    defaultString = "localhost.localdomain"
            ) String hostname,
            @PluginAttribute(
                    value = "appName",
                    defaultString = "jla-06"
            ) String appName,
            @PluginAttribute(
                    value = "readTimeout",
                    defaultInt = 1500
            ) int readTimeout,
            @PluginAttribute(
                    value = "writeTimeout",
                    defaultInt = 1500
            ) int writeTimeout,
            @PluginAttribute(
                    value = "reconnectInterval",
                    defaultInt = 500
            ) int reconnectInterval,
            @PluginAttribute(
                    value = "connectionTimeout",
                    defaultInt = 1500
            ) int connectionTimeout,
            @PluginAttribute(
                    value = "useSD",
                    defaultBoolean = true
            ) boolean useSD,
            @PluginAttribute(
                    value = "relpAddress",
                    defaultString = "127.0.0.1"
            ) String relpAddress,
            @PluginAttribute(
                    value = "relpPort",
                    defaultInt = 1601
            ) int relpPort,
            @PluginAttribute(
                    value = "useTLS",
                    defaultBoolean = false
            ) boolean useTLS,
            @PluginAttribute(
                    value = "keystorePath",
                    defaultString = "/unset/path/to/keystore"
            ) String keystorePath,
            @PluginAttribute(
                    value = "keystorePassword",
                    defaultString = ""
            ) String keystorePassword,
            @PluginAttribute(
                    value = "tlsProtocol",
                    defaultString = "TLSv1.3"
            ) String tlsProtocol,
            @PluginAttribute(
                    value = "enableSystemID",
                    defaultBoolean = false
            ) boolean enableSystemID,
            @PluginAttribute(
                    value = "systemID",
                    defaultString = ""
            ) String systemID,
            @PluginAttribute(
                    value = "rebindRequestAmount",
                    defaultInt = 100_000
            ) int rebindRequestAmount,
            @PluginAttribute(
                    value = "rebindEnabled",
                    defaultBoolean = true
            ) boolean rebindEnabled,
            @PluginAttribute(
                    value = "reconnectIfNoMessagesInterval",
                    defaultInt = 150_000
            ) int reconnectIfNoMessagesInterval,
            @PluginAttribute(
                    value = "synchronizedAccess",
                    defaultBoolean = false
            ) boolean synchronizedAccess,
            @PluginElement("Layout") Layout layout,
            @PluginElement("Filters") Filter filter
    ) {

        return new RelpAppender(
                name,
                filter,
                layout,
                ignoreExceptions,
                null,
                hostname,
                appName,
                readTimeout,
                writeTimeout,
                reconnectInterval,
                connectionTimeout,
                useSD,
                relpAddress,
                relpPort,
                useTLS,
                keystorePath,
                keystorePassword,
                tlsProtocol,
                enableSystemID,
                systemID,
                rebindRequestAmount,
                rebindEnabled,
                reconnectIfNoMessagesInterval,
                synchronizedAccess
        );
    }

    @Override
    public void stop() {
        super.stop();
        relpLogAppender.stop();
    }
}
