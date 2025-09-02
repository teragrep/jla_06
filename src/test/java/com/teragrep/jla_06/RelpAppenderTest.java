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

import com.teragrep.jla_06.server.TestServer;
import com.teragrep.jla_06.server.TestServerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class RelpAppenderTest {

    @Test
    void testNormalUsage() throws Exception {
        TestServerFactory serverFactory = new TestServerFactory();

        final int serverPort = 1601;

        final String testPayload = "some payload";

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        AtomicLong openCount = new AtomicLong();
        AtomicLong closeCount = new AtomicLong();

        try (TestServer server = serverFactory.create(serverPort, messageList, openCount, closeCount)) {
            server.run();
            RelpAppender relpAppender = createRelpAppender();
            relpAppender.start();
            Log4jLogEvent.newBuilder().setMessage(null).build();
            relpAppender
                    .append(Log4jLogEvent.newBuilder().setMessage(new SimpleMessage(testPayload)).setThreadName("ThreadXyz").setLoggerName("LoggerXyz").setLevel(Level.INFO).setTimeMillis(1).build());
        }
        Assertions.assertEquals(1, messageList.size(), "messageList size not expected");
        byte[] message = messageList.getFirst();
        System.out.println("Message: " + new String(message, StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(testPayload.getBytes(StandardCharsets.UTF_8), message, "payload not expected");
    }

    private RelpAppender createRelpAppender() {
        Layout<String> layout = PatternLayout
                .newBuilder()
                .withPattern("%d{dd.MM.yyyy HH:mm:ss.SSS} [%level] [%logger] [%thread] %msg%ex%n")
                .build();
        return RelpAppender
                .createAppender(
                        "relpAppender", false, "jla_06", "jla_06", 5000, 5000, 5000, 5000, true, "127.0.0.1", 1601,
                        false, null, null, null, layout, null
                );
    }
}
