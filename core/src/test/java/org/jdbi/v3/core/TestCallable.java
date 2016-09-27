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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.Types;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class TestCallable
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle h;


    @Before
    public void setUp() throws Exception {
        h = db.getJdbi().open();
        h.execute("CREATE ALIAS TO_DEGREES FOR \"java.lang.Math.toDegrees\"");
        h.execute("CREATE ALIAS TEST_PROCEDURE FOR \"org.jdbi.v3.core.TestCallable.testProcedure\"");
    }

    @Test
    public void testStatement() throws Exception {
        OutParameters ret = h.createCall("? = CALL TO_DEGREES(?)")
                .registerOutParameter(0, Types.DOUBLE)
                .bind(1, 100.0d)
                .invoke();

        // JDBI oddity : register or bind is 0-indexed, which JDBC is 1-indexed.
        Double expected = Math.toDegrees(100.0d);
        assertThat(ret.getDouble(1)).isEqualTo(expected, Offset.offset(0.001));
        assertThat(ret.getLong(1).longValue()).isEqualTo(expected.longValue());
        assertThat(ret.getShort(1).shortValue()).isEqualTo(expected.shortValue());
        assertThat(ret.getInt(1).intValue()).isEqualTo(expected.intValue());
        assertThat(ret.getFloat(1).floatValue()).isEqualTo(expected.floatValue(), Offset.offset(0.001f));

        try {
            ret.getDate(1);
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            //e.printStackTrace();
        }

        try {
            ret.getDate(2);
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            //e.printStackTrace();
        }

    }

    @Test
    public void testStatementWithNamedParam() throws Exception {
        OutParameters ret = h.createCall(":x = CALL TO_DEGREES(:y)")
                .registerOutParameter("x", Types.DOUBLE)
                .bind("y", 100.0d)
                .invoke();

        Double expected = Math.toDegrees(100.0d);
        assertThat(ret.getDouble("x")).isEqualTo(expected, Offset.offset(0.001));
        assertThat(ret.getLong("x").longValue()).isEqualTo(expected.longValue());
        assertThat(ret.getShort("x").shortValue()).isEqualTo(expected.shortValue());
        assertThat(ret.getInt("x").intValue()).isEqualTo(expected.intValue());
        assertThat(ret.getFloat("x")).isEqualTo(expected.floatValue());

        try {
            ret.getDate("x");
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            //e.printStackTrace();
        }

        try {
            ret.getDate("y");
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            assertThat(true).isTrue();
        }
    }

    @Test
    @Ignore // TODO(scs): how do we test out parameters with h2?
    public void testWithNullReturn() throws Exception {
        OutParameters ret = h.createCall("CALL TEST_PROCEDURE(?, ?)")
                .bind(0, (String)null)
                .registerOutParameter(1, Types.VARCHAR)
                .invoke();

        // JDBI oddity : register or bind is 0-indexed, which JDBC is 1-indexed.
        String out = ret.getString(2);
        assertThat(out).isNull();
    }

    @Test
    @Ignore // TODO(scs): how do we test out parameters with h2?
    public void testWithNullReturnWithNamedParam() throws Exception {
        OutParameters ret = h.createCall("CALL TEST_PROCEDURE(:x, :y)")
                .bind("x", (String)null)
                .registerOutParameter("y", Types.VARCHAR)
                .invoke();

        String out = ret.getString("y");
        assertThat(out).isNull();
    }

    public static void testProcedure(String in, String[] out) {
        out = new String[1];
        out[0] = in;
    }
}
