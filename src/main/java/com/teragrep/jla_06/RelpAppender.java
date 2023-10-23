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

import com.teragrep.rlo_14.Facility;
import com.teragrep.rlo_14.SDElement;
import com.teragrep.rlo_14.Severity;
import com.teragrep.rlo_14.SyslogMessage;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;
import com.teragrep.rlp_01.SSLContextFactory;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Plugin(name="RelpAppender", category=Core.CATEGORY_NAME, elementType=Appender.ELEMENT_TYPE, printObject=true)
public class RelpAppender extends AbstractAppender {
    private RelpConnection relpConnection;
    private RelpBatch batch;
    String appName;
    int connectionTimeout;
    String hostname;
    int readTimeout;
    String relpAddress;
    boolean useSD;
    int relpPort;
    int writeTimeout;
    int reconnectInterval;
    boolean connected = false;
    SSLContext sslContext;

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getRelpAddress() {
        return this.relpAddress;
    }

    public void setRelpAddress(String address) {
        this.relpAddress = address;
    }

    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getWriteTimeout() {
        return this.writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRelpPort() {
        return this.relpPort;
    }

    public void setRelpPort(int port) {
        this.relpPort = port;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String app) {
        this.appName = app;
    }

    public void setUseSD(Boolean useSD){
        this.useSD = useSD;
    }

    public boolean getUseSD(){
        return this.useSD;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    protected RelpAppender(String name, Filter filter, Layout<? extends Serializable> layout,  boolean ignoreExceptions, Property[] properties, String hostname, String appName, int readTimeout, int writeTimeout, int reconnectInterval, int connectionTimeout, boolean useSD, String relpAddress, int relpPort, SSLContext sslContext) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.setHostname(hostname);
        this.setAppName(appName);
        this.setReadTimeout(readTimeout);
        this.setWriteTimeout(writeTimeout);
        this.setReconnectInterval(reconnectInterval);
        this.setConnectionTimeout(connectionTimeout);
        this.setUseSD(useSD);
        this.setRelpAddress(relpAddress);
        this.setRelpPort(relpPort);
        this.setSslContext(sslContext);
        if (sslContext == null) {
            this.relpConnection = new RelpConnection();
        }
        else {
            SSLEngine sslEngine = sslContext.createSSLEngine();
            Supplier<SSLEngine> sslEngineSupplier = sslContext::createSSLEngine;
            this.relpConnection = new RelpConnection(sslEngineSupplier);
        }
        connect();
    }

    @Override
    public void append(LogEvent event) {
        if (!this.connected) {
            connect();
        }

        // Craft syslog message
        SyslogMessage syslog = new SyslogMessage()
                .withTimestamp(new Date().getTime())
                .withSeverity(Severity.WARNING)
                .withAppName(this.getAppName())
                .withHostname(this.getHostname())
                .withFacility(Facility.USER)
                .withMsg(new String(getLayout().toByteArray(event), StandardCharsets.UTF_8));

        // Add SD if enabled
        if (this.getUseSD()) {
            SDElement event_id_48577 = new SDElement("event_id@48577")
                    .addSDParam("hostname", this.getHostname())
                    .addSDParam("uuid", UUID.randomUUID().toString())
                    .addSDParam("source", "source")
                    .addSDParam("unixtime", Long.toString(System.currentTimeMillis()));
            SDElement origin_48577 = new SDElement("origin@48577")
                    .addSDParam("hostname", this.getHostname());
            syslog = syslog
                    .withSDElement(event_id_48577)
                    .withSDElement(origin_48577);
        }

        RelpBatch batch = new RelpBatch();
        batch.insert(syslog.toRfc5424SyslogMessage().getBytes(StandardCharsets.UTF_8));

        boolean allSent = false;
        while (!allSent) {
            try {
                this.relpConnection.commit(batch);
            } catch (IllegalStateException | IOException | java.util.concurrent.TimeoutException e) {
                System.out.println("RelpAppender.flush.commit> exception:");
                e.printStackTrace();
                this.relpConnection.tearDown();
                this.connected = false;
            }
            // Check if everything has been sent, retry and reconnect if not.
            if (!batch.verifyTransactionAll()) {
                batch.retryAllFailed();
                try {
                    reconnect();
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            } else {
                allSent = true;
            }
        }
    }

    @PluginFactory
    public static RelpAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
            @PluginAttribute("hostname") String hostname,
            @PluginAttribute("appName") String appName,
            @PluginAttribute("readTimeout") int readTimeout,
            @PluginAttribute("writeTimeout") int writeTimeout,
            @PluginAttribute("reconnectInterval") int reconnectInterval,
            @PluginAttribute("connectionTimeout") int connectionTimeout,
            @PluginAttribute("useSD") boolean useSD,
            @PluginAttribute("relpAddress") String relpAddress,
            @PluginAttribute("relpPort") int relpPort,
            @PluginAttribute("useTLS") boolean useTLS,
            @PluginAttribute("keystorePath") String keystorePath,
            @PluginAttribute("keystorePassword") String keystorePassword,
            @PluginAttribute("tlsProtocol") String tlsProtocol,
            @PluginElement("Layout") Layout layout,
            @PluginElement("Filters") Filter filter) {

        SSLContext sslContext = null;
        if (useTLS) {
            try {
                sslContext = SSLContextFactory.authenticatedContext(keystorePath, keystorePassword, tlsProtocol);
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        return new RelpAppender(name, filter, layout, ignoreExceptions, null, hostname, appName, readTimeout, writeTimeout, reconnectInterval, connectionTimeout, useSD, relpAddress, relpPort, sslContext);
    }

    private void reconnect() throws IOException, TimeoutException {
        disconnect();
        connect();
    }

    private void disconnect() throws IOException, TimeoutException {
        if (!this.connected) {
            return;
        }
        try {
            this.relpConnection.disconnect();
        } catch (IllegalStateException | IOException | java.util.concurrent.TimeoutException e) {
            System.out.println("RelpAppender.disconnect> exception:");
            e.printStackTrace();
        }
        this.relpConnection.tearDown();
        this.connected = false;
    }

    private void connect() {
        while (!this.connected) {
            try {
                this.connected = this.relpConnection.connect(this.getRelpAddress(), this.getRelpPort());
            } catch (Exception e) {
                System.out.println("RelpAppender.connect> exception:");
                e.printStackTrace();
            }
            if(!this.connected) {
                try {
                    Thread.sleep(this.getReconnectInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        try {
            disconnect();
            return true;
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }
}
