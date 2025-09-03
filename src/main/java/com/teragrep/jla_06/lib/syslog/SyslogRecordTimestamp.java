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
package com.teragrep.jla_06.lib.syslog;

import com.teragrep.rlo_14.SyslogMessage;

import java.time.Instant;

public final class SyslogRecordTimestamp implements SyslogRecord {

    private final SyslogRecord syslogRecord;
    private final Instant timestamp;

    public SyslogRecordTimestamp(SyslogRecord syslogRecord) {
        this(syslogRecord, Instant.now());
    }

    public SyslogRecordTimestamp(SyslogRecord syslogRecord, Instant timestamp) {
        this.syslogRecord = syslogRecord;
        this.timestamp = timestamp;
    }

    @Override
    public SyslogMessage asSyslogMessage() {
        return syslogRecord.asSyslogMessage().withTimestamp(timestamp.toEpochMilli());
    }
}
