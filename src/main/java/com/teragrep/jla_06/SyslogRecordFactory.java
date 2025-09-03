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

import com.teragrep.jla_06.lib.syslog.SyslogRecord;
import com.teragrep.jla_06.lib.syslog.SyslogRecordConfigured;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithEventID;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithOrigin;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithPayload;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithSystemID;
import com.teragrep.jla_06.lib.syslog.SyslogRecordWithTimestamp;

public class SyslogRecordFactory {

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
