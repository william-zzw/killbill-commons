/*
 * Copyright 2004-2014 Brian McCallister
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;

class InTransactionWithIsolationLevelHandler implements Handler
{
    @Override
    public Object invoke(HandleDing h, final Object target, Object[] args, MethodProxy mp)
    {
        h.retain("transaction#withlevel");
        try {
            final TransactionIsolationLevel level = (TransactionIsolationLevel) args[0];
            final Transaction t = (Transaction) args[1];
            return h.getHandle().inTransaction(level, new TransactionCallback()
            {
                @Override
                public Object inTransaction(Handle conn, TransactionStatus status) throws Exception
                {
                    return t.inTransaction(target, status);
                }
            });
        }
        finally {
            h.release("transaction#withlevel");
        }
    }
}
