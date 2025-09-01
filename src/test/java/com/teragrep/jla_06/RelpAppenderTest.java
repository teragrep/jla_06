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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class RelpAppenderTest {

    @Test
    void testNormalUsage() {
        TestServerFactory serverFactory = new TestServerFactory();

        final int serverPort = 1601;

        final String testPayload = "some payload";

        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        AtomicLong openCount = new AtomicLong();
        AtomicLong closeCount = new AtomicLong();

        Assertions.assertDoesNotThrow(() -> {
            try (TestServer server = serverFactory.create(serverPort, messageList, openCount, closeCount)) {
                server.run();
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                File file = new File("src/test/resources/log4j2-non-standard-location.xml");
                context.setConfigLocation(file.toURI());
                final Logger logger = LogManager.getLogger(this.getClass());
                logger.info(testPayload);
            }

        });

        Assertions.assertEquals(1, messageList.size(), "messageList size not expected");

    }

}
