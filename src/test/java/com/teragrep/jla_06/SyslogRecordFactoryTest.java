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

import com.teragrep.rlo_14.SDElement;
import com.teragrep.rlo_14.SyslogMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

public class SyslogRecordFactoryTest {

    @Test
    @DisplayName(value = "Tests SyslogRecordFactory with useSD=true and enableSystemID=true")
    public void testSyslogRecordFactoryWithAllFields() {
        SyslogRecordFactory syslogRecordFactory = new SyslogRecordFactory(
                "testhost",
                "testapp",
                "testhost",
                true,
                true,
                "testhost-ci"
        );
        SyslogMessage syslogMessage = syslogRecordFactory.create("Example Payload").asSyslogMessage();
        Assertions.assertEquals("testhost", syslogMessage.getHostname());
        Assertions.assertEquals("testapp", syslogMessage.getAppName());
        Assertions.assertEquals("Example Payload", syslogMessage.getMsg());

        SDElement businessSystemSDElement = new SDElement("businessSystem@48577").addSDParam("systemId", "testhost-ci");
        Assertions.assertTrue(syslogMessage.getSDElements().contains(businessSystemSDElement));

        SDElement originSDElement = new SDElement("origin@48577").addSDParam("hostname", "test-host");
        Assertions.assertTrue(syslogMessage.getSDElements().contains(originSDElement));
    }

    @Test
    @DisplayName(value = "Tests SyslogRecordFactory with useSD=false and enableSystemID=false")
    public void testSyslogRecordFactoryWithNoExtraFields() {
        SyslogRecordFactory syslogRecordFactory = new SyslogRecordFactory(
                "testhost",
                "testapp",
                "testhost",
                false,
                false,
                ""
        );
        SyslogMessage syslogMessage = syslogRecordFactory.create("Example Payload").asSyslogMessage();
        Assertions.assertEquals("testhost", syslogMessage.getHostname());
        Assertions.assertEquals("testapp", syslogMessage.getAppName());
        Assertions.assertEquals("Example Payload", syslogMessage.getMsg());
        Assertions.assertTrue(syslogMessage.getSDElements().isEmpty());
    }
}
