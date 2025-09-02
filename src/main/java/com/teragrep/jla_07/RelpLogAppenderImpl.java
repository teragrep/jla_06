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
package com.teragrep.jla_07;

import com.teragrep.jla_07.syslog.*;
import com.teragrep.rlp_01.client.IManagedRelpConnection;
import com.teragrep.rlp_01.pool.Pool;

import java.nio.charset.StandardCharsets;

public final class RelpLogAppenderImpl implements RelpLogAppender {

    private final Pool<IManagedRelpConnection> relpConnectionPool;

    public RelpLogAppenderImpl(Pool<IManagedRelpConnection> relpConnectionPool) {
        this.relpConnectionPool = relpConnectionPool;
    }

    @Override
    public void append(SyslogRecord syslogRecord) {
        IManagedRelpConnection connection = relpConnectionPool.get();

        connection.ensureSent(syslogRecord.getRecord().toRfc5424SyslogMessage().getBytes(StandardCharsets.UTF_8));
        relpConnectionPool.offer(connection);
    }

    @Override
    public void stop() {
        relpConnectionPool.close();
    }

    @Override
    public boolean isStub() {
        return false;
    }
}
