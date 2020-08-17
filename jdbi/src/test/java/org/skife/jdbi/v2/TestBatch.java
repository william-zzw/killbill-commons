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
package org.skife.jdbi.v2;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@Category(JDBITests.class)
public class TestBatch extends DBITestCase
{
    @Test
    public void testBasics() throws Exception
    {
        Handle h = this.openHandle();

        Batch b = h.createBatch();
        b.add("insert into something (id, name) values (0, 'Keith')");
        b.add("insert into something (id, name) values (1, 'Eric')");
        b.add("insert into something (id, name) values (2, 'Brian')");
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").map(Something.class).list();
        assertEquals(3, r.size());
    }
}
