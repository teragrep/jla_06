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
import com.teragrep.rlo_06.RFC5424Frame;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

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
            relpAppender.stop();
        }
        Assertions.assertEquals(1, messageList.size(), "messageList size not expected");

        for (byte[] message : messageList) {
            RFC5424Frame rfc5424Frame = new RFC5424Frame();
            rfc5424Frame.load(new ByteArrayInputStream(message));

            AtomicBoolean hasNext = new AtomicBoolean();
            Assertions.assertDoesNotThrow(() -> hasNext.set(rfc5424Frame.next()));
            Assertions.assertTrue(hasNext.get());

            Assertions.assertEquals("jla_06", rfc5424Frame.hostname.toString());
            Assertions.assertEquals("jla_06", rfc5424Frame.appName.toString());

            Pattern timestampPattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z");

            Assertions.assertTrue(timestampPattern.matcher(rfc5424Frame.timestamp.toString()).matches());

            Assertions.assertEquals(testPayload, rfc5424Frame.msg.toString());
        }

        Assertions.assertEquals(1, openCount.get(), "openCount not expected");
        Assertions.assertEquals(1, closeCount.get(), "closeCount not expected");
    }

    private RelpAppender createRelpAppender() {
        Layout<String> layout = PatternLayout.newBuilder().withPattern("%msg").build();
        return RelpAppender
                .createAppender(
                        "relpAppender", false, "jla_06", "jla_06", 5000, 5000, 5000, 5000, true, "127.0.0.1", 1601,
                        false, null, null, null, layout, null
                );
    }
}
