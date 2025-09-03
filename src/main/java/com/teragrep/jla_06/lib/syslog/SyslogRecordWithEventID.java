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

import com.teragrep.rlo_14.SDElement;
import com.teragrep.rlo_14.SyslogMessage;

import java.util.Objects;
import java.util.UUID;

public final class SyslogRecordWithEventID implements SyslogRecord {

    private final SyslogRecord syslogRecord;
    private final String hostname;

    public SyslogRecordWithEventID(SyslogRecord syslogRecord, String hostname) {
        this.syslogRecord = syslogRecord;
        this.hostname = hostname;
    }

    @Override
    public SyslogMessage asSyslogMessage() {
        final SDElement eventIdSDE = new SDElement("event_id@48577");

        eventIdSDE.addSDParam("hostname", hostname);

        String uuid = UUID.randomUUID().toString();
        eventIdSDE.addSDParam("uuid", uuid);
        eventIdSDE.addSDParam("source", "source");

        long unixtime = System.currentTimeMillis();
        String epochtime = Long.toString(unixtime);
        eventIdSDE.addSDParam("unixtime", epochtime);

        return syslogRecord.asSyslogMessage().withSDElement(eventIdSDE);
    }

    @Override
    public int hashCode() {
        return Objects.hash(syslogRecord, hostname);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyslogRecordWithEventID other = (SyslogRecordWithEventID) o;
        return Objects.equals(this.syslogRecord, other.syslogRecord) && Objects.equals(this.hostname, other.hostname);
    }
}
