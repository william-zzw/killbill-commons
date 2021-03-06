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

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.JDBITests;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.IntegerMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

@Category(JDBITests.class)
public class TestPostgresBugs {

    private static DBI createDbi() {
        String user = System.getenv("POSTGRES_USER");
        String pass = System.getenv("POSTGRES_PASS");
        String url = System.getenv("POSTGRES_URL");

        assumeThat(user, notNullValue());
        //        assumeThat(pass, notNullValue());
        assumeThat(url, notNullValue());

        final DBI dbi = new DBI(url, user, pass);
        dbi.registerMapper(new SomethingMapper());

        return dbi;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        createDbi().withHandle(new HandleCallback<Object>() {
            @Override
            public Object withHandle(Handle handle) throws Exception {
                handle.execute("create table if not exists something (id int primary key, name varchar(100))");
                handle.execute("delete from something");
                return null;
            }
        });
    }

    @Test
    public void testConnected() throws Exception {
        DBI dbi = createDbi();
        int four = dbi.withHandle(new HandleCallback<Integer>() {
            @Override
            public Integer withHandle(Handle handle) throws Exception {
                return handle.createQuery("select 2 + 2").map(IntegerMapper.FIRST).first();
            }
        });

        assertThat(four, equalTo(4));
    }

    @Test
    public void testTransactions() throws Exception {
        DBI dbi = createDbi();
        Dao dao = dbi.onDemand(Dao.class);

        dao.begin();
        Something s = dao.insertAndFetch(1, "Brian");
        dao.commit();
        assertThat(s, equalTo(new Something(1, "Brian")));
    }

    @Test
    public void testExplicitBeginAndInTransaction() throws Exception {
        DBI dbi = createDbi();
        Dao dao = dbi.onDemand(Dao.class);

        dao.begin();
        Something s = dao.inTransaction(new org.skife.jdbi.v2.Transaction<Something, Dao>() {

            @Override
            public Something inTransaction(Dao transactional, TransactionStatus status) throws Exception {
                return transactional.insertAndFetch(1, "Brian");
            }
        });

        dao.commit();
        assertThat(s, equalTo(new Something(1, "Brian")));
    }

    public static abstract class Dao implements Transactional<Dao> {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public abstract void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        public abstract Something findById(@Bind("id") int id);

        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        public Something insertAndFetch(int id, String name) {
            insert(id, name);
            return findById(id);
        }

        @Transaction
        public Something insertAndFetchWithNestedTransaction(int id, String name) {
            return insertAndFetch(id, name);
        }

        @Transaction
        public Something failed(int id, String name) throws IOException {
            insert(id, name);
            throw new IOException("woof");
        }
    }
}
