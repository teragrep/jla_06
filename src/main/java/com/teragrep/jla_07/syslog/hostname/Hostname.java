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
package com.teragrep.jla_07.syslog.hostname;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class Hostname {

    private final String defaultHostname;

    public Hostname(final String defaultHostname) {
        this.defaultHostname = defaultHostname;
    }

    public String hostname() {
        String rv;
        try {
            rv = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            rv = defaultHostname;
            System.err.println("Could not determine hostname, defaulting to <[" + defaultHostname + "]>");
        }
        return rv;
    }
}
