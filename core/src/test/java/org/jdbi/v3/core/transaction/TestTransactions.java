/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.exception.JdbiException;
import org.jdbi.v3.core.exception.TransactionException;
import org.jdbi.v3.core.exception.TransactionFailedException;
import org.junit.Rule;
import org.junit.Test;

public class TestTransactions
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testCallback() throws Exception
    {
        Handle h = db.openHandle();

        String woot = h.inTransaction((handle, status) -> "Woot!");

        assertThat(woot).isEqualTo("Woot!");
    }

    @Test
    public void testRollbackOutsideTx() throws Exception
    {
        Handle h = db.openHandle();

        h.insert("insert into something (id, name) values (?, ?)", 7, "Tom");
        h.rollback();
    }

    @Test
    public void testDoubleOpen() throws Exception
    {
        Handle h = db.openHandle();
        assertThat(h.getConnection().getAutoCommit()).isTrue();

        h.begin();
        h.begin();
        assertThat(h.getConnection().getAutoCommit()).isFalse();
        h.commit();
        assertThat(h.getConnection().getAutoCommit()).isTrue();
    }

    @Test
    public void testExceptionAbortsTransaction() throws Exception
    {
        Handle h = db.openHandle();

        try
        {
            h.inTransaction((handle, status) -> {
                handle.insert("insert into something (id, name) values (:id, :name)", 0, "Keith");

                throw new IOException();
            });
            fail("Should have thrown exception");
        }
        catch (IOException expected) {
        }

        List<Something> r = h.createQuery("select * from something").mapToBean(Something.class).list();
        assertThat(r).isEmpty();
    }

    @Test
    public void testRollbackOnlyAbortsTransaction() throws Exception
    {
        Handle h = db.openHandle();

        try
        {
            h.inTransaction((handle, status) -> {
                handle.insert("insert into something (id, name) values (:id, :name)", 0, "Keith");
                status.setRollbackOnly();
                return "Hi";
            });
            fail("Should have thrown exception");
        }
        catch (TransactionFailedException expected)
        {
            assertThat(expected).isNotNull();
        }

        List<Something> r = h.createQuery("select * from something").mapToBean(Something.class).list();
        assertThat(r).isEmpty();
    }

    @Test
    public void testCheckpoint() throws Exception
    {
        Handle h = db.openHandle();
        h.begin();

        h.insert("insert into something (id, name) values (:id, :name)", 1, "Tom");
        h.checkpoint("first");
        h.insert("insert into something (id, name) values (:id, :name)", 2, "Martin");
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly())
                .isEqualTo(Integer.valueOf(2));
        h.rollback("first");
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly())
                .isEqualTo(Integer.valueOf(1));
        h.commit();
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly())
                .isEqualTo(Integer.valueOf(1));
    }

    @Test
    public void testReleaseCheckpoint() throws Exception
    {
        Handle h = db.openHandle();
        h.begin();
        h.checkpoint("first");
        h.insert("insert into something (id, name) values (:id, :name)", 1, "Martin");

        h.release("first");

        try {
            h.rollback("first");
            fail("Should have thrown an exception of some kind");
        }
        catch (TransactionException e) {
            assertThat(e).isNotNull();
            h.rollback();
        }
    }

    @Test
    public void testThrowingRuntimeExceptionPercolatesOriginal() throws Exception
    {
        Handle h = db.openHandle();
        try {
            h.inTransaction((handle, status) -> {
                throw new IllegalArgumentException();
            });
        }
        catch (JdbiException e) {
            fail("Should have thrown a straight RuntimeException");
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e).isNotNull();
        }
        catch (Exception e) {
            fail("Should have been caught at IllegalArgumentException");
        }
    }
}
