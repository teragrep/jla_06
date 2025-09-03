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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelpLogAppenderTest {

    @Test
    void testNormalUsage() {
        TestServerFactory serverFactory = new TestServerFactory();

        final int serverPort = 1601;

        final String hostname = "jla-06-normal";
        final String appName = "jla-06-normal";
        final String testPayload = "some payload";

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        AtomicLong openCount = new AtomicLong();
        AtomicLong closeCount = new AtomicLong();

        Assertions.assertDoesNotThrow(() -> {
            try (TestServer server = serverFactory.create(serverPort, messageList, openCount, closeCount)) {
                server.run();
                RelpAppender relpAppender = createRelpAppender(hostname, appName);
                relpAppender.start();
                relpAppender
                        .append(Log4jLogEvent.newBuilder().setMessage(new SimpleMessage(testPayload)).setThreadName("ThreadXyz").setLoggerName("LoggerXyz").setLevel(Level.INFO).setTimeMillis(1).build());
                relpAppender.stop();
            }
        });
        Assertions.assertEquals(1, messageList.size(), "messageList size not expected");

        for (byte[] message : messageList) {
            RFC5424Frame rfc5424Frame = new RFC5424Frame();
            rfc5424Frame.load(new ByteArrayInputStream(message));

            AtomicBoolean hasNext = new AtomicBoolean();
            Assertions.assertDoesNotThrow(() -> hasNext.set(rfc5424Frame.next()));
            Assertions.assertTrue(hasNext.get());

            Assertions.assertEquals(hostname, rfc5424Frame.hostname.toString());
            Assertions.assertEquals(appName, rfc5424Frame.appName.toString());

            Pattern timestampPattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z");

            Assertions.assertTrue(timestampPattern.matcher(rfc5424Frame.timestamp.toString()).matches());

            Assertions.assertEquals(testPayload, rfc5424Frame.msg.toString());
        }

        Assertions.assertEquals(1, openCount.get(), "openCount not expected");
        Assertions.assertEquals(1, closeCount.get(), "closeCount not expected");
    }

    @Test
    public void threadedTest() {
        TestServerFactory serverFactory = new TestServerFactory();

        final int serverPort = 1601;
        final int testCycles = 10_000;
        final String hostname = "jla-06-threaded";
        final String appName = "jla-06-threaded";
        final String testPayload = "some payload";

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        AtomicLong openCount = new AtomicLong();
        AtomicLong closeCount = new AtomicLong();

        Assertions.assertDoesNotThrow(() -> {
            try (TestServer server = serverFactory.create(serverPort, messageList, openCount, closeCount)) {
                server.run();
                RelpAppender relpAppender = createRelpAppender(hostname, appName);
                relpAppender.start();
                CountDownLatch countDownLatch = new CountDownLatch(testCycles);
                for (int i = 0; i < testCycles; i++) {
                    final String testString = testPayload + " " + i;
                    ForkJoinPool.commonPool().submit(() -> {
                        relpAppender
                                .append(Log4jLogEvent.newBuilder().setMessage(new SimpleMessage(testString)).setThreadName("ThreadXyz").setLoggerName("LoggerXyz").setLevel(Level.INFO).setTimeMillis(1).build());
                        countDownLatch.countDown();
                    });
                }

                countDownLatch.await();
                relpAppender.stop();

            }
        });

        Assertions.assertEquals(testCycles, messageList.size(), "messageList size not expected");

        Pattern pattern = Pattern.compile(testPayload + " (\\d+)");

        Map<Integer, Boolean> testIterationsMap = new HashMap<>();
        for (int i = 0; i < testCycles; i++) {
            testIterationsMap.put(i, true);
        }

        for (byte[] message : messageList) {
            RFC5424Frame rfc5424Frame = new RFC5424Frame();
            rfc5424Frame.load(new ByteArrayInputStream(message));

            AtomicBoolean frameNext = new AtomicBoolean();
            Assertions.assertDoesNotThrow(() -> {
                frameNext.set(rfc5424Frame.next());
            });
            Assertions.assertTrue(frameNext.get());

            Assertions.assertEquals(hostname, rfc5424Frame.hostname.toString());
            Assertions.assertEquals(appName, rfc5424Frame.appName.toString());

            Matcher matcher = pattern.matcher(rfc5424Frame.msg.toString());
            boolean matches = matcher.matches();
            Assertions.assertTrue(matches, "payload unexpected");

            String testIterationValue = matcher.group(1);

            int testIteration = Assertions.assertDoesNotThrow(() -> {
                return Integer.parseInt(testIterationValue);
            }, "extracted test iteration not integer");

            Boolean iterationValue = testIterationsMap.remove(testIteration);
            Assertions.assertNotNull(iterationValue);
            Assertions.assertTrue(iterationValue);
        }
        Assertions
                .assertTrue(
                        testIterationsMap.isEmpty(),
                        "testIterationsMap was not empty: some messages were not delivered successfully"
                );

        Assertions.assertTrue(openCount.get() >= 1, "openCount not expected");
        Assertions.assertTrue(closeCount.get() >= 1, "closeCount not expected");
    }

    private RelpAppender createRelpAppender(String hostname, String appName) {
        Layout<String> layout = PatternLayout.newBuilder().withPattern("%msg").build();
        return RelpAppender
                .createAppender(
                        "relpAppender", false, hostname, appName, 5000, 5000, 5000, 5000, true, "127.0.0.1", 1601,
                        false, null, null, null, false, "", 1000, false, 1000, false, layout, null
                );
    }
}
