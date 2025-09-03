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

import com.teragrep.jla_06.lib.syslog.SyslogRecordConfigured;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithEventID;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithOrigin;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithPayload;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithSystemID;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithTimestamp;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EqualityTests {

    @Test
    @DisplayName(value = "Test SyslogRecordConfigured equalness")
    public void testSyslogRecordConfigured() {
        EqualsVerifier.forClass(SyslogRecordConfigured.class).verify();
    }

    @Test
    @DisplayName(value = "Test SyslogRecordWithEventID equalness")
    public void testSyslogRecordWithEventID() {
        EqualsVerifier.forClass(SyslogRecordWithEventID.class).verify();
    }

    @Test
    @DisplayName(value = "Test SyslogRecordWithOrigin equalness")
    public void testSyslogRecordWithOrigin() {
        EqualsVerifier.forClass(SyslogRecordWithOrigin.class).verify();
    }

    @Test
    @DisplayName(value = "Test SyslogRecordWithPayload equalness")
    public void testSyslogRecordWithPayload() {
        EqualsVerifier.forClass(SyslogRecordWithPayload.class).verify();
    }

    @Test
    @DisplayName(value = "Test SyslogRecordWithSystemID equalness")
    public void testSyslogRecordWithSystemID() {
        EqualsVerifier.forClass(SyslogRecordWithSystemID.class).verify();
    }

    @Test
    @DisplayName(value = "Test SyslogRecordWithTimestamp equalness")
    public void testSyslogRecordWithTimestamp() {
        EqualsVerifier.forClass(SyslogRecordWithTimestamp.class).verify();
    }
}
