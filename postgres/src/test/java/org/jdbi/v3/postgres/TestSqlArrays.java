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
package org.jdbi.v3.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.junit.*;

public class TestSqlArrays {
    private static final String U_SELECT = "SELECT u FROM uuids";
    private static final String U_INSERT = "INSERT INTO uuids VALUES(:uuids, NULL)";
    private static final String I_SELECT = "SELECT i FROM uuids";
    private static final String I_INSERT = "INSERT INTO uuids VALUES(NULL, :ints)";

    @ClassRule
    public static PostgresDbRule db = new PostgresDbRule();

    private Handle h;
    private ArrayObject ao;

    @Before
    public void setUp() {
        h = db.getSharedHandle();
        h.useTransaction((th, status) -> {
            th.execute("DROP TABLE IF EXISTS uuids");
            th.execute("CREATE TABLE uuids (u UUID[], i INT[])");
        });
        ao = h.attach(ArrayObject.class);
    }

    private final UUID[] testUuids = new UUID[] {
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
    };

    private final int[] testInts = new int[] {
        5, 4, -6, 1, 9, Integer.MAX_VALUE, Integer.MIN_VALUE
    };

    @Test
    public void testUuidArray() throws Exception {
        ao.insertUuidArray(testUuids);
        assertThat(ao.fetchUuidArray()).containsExactly(testUuids);
    }

    @Test
    @Ignore("Inserting lists to arrays is not supported")
    public void testUuidList() throws Exception {
        ao.insertUuidList(Arrays.asList(testUuids));
        assertThat(ao.fetchUuidList()).containsExactly(testUuids);
    }

    @Test
    public void testIntArray() throws Exception {
        ao.insertIntArray(testInts);
        int[] actuals = ao.fetchIntArray();
        assertThat(actuals).containsExactly(testInts);
    }

    @Test
    public void testEmptyIntArray() throws Exception {
        ao.insertIntArray(new int[0]);
        assertThat(ao.fetchIntArray()).isEmpty();
    }

    @Test
    public void testBoxedIntArray() throws Exception {
        Integer[] source = IntStream.of(testInts).mapToObj(Integer::valueOf).toArray(Integer[]::new);
        ao.insertBoxedIntArray(source);
        Integer[] actuals = ao.fetchBoxedIntArray();
        assertThat(actuals).containsExactly(actuals);
    }

    @Test
    public void testObjectArray() throws Exception {
        ao.insertIntArray(testInts);
        Object[] actuals = ao.fetchObjectArray();
        Object[] expecteds = IntStream.of(testInts).mapToObj(Integer::valueOf).toArray(Object[]::new);
        assertThat(actuals).containsExactly(expecteds);
    }

    @Test
    @Ignore("Inserting lists to arrays is not supported")
    public void testIntList() throws Exception {
        List<Integer> testIntList = new ArrayList<Integer>();
        Arrays.stream(testInts).forEach(testIntList::add);
        ao.insertIntList(testIntList);
        assertThat(ao.fetchIntList()).isEqualTo(testIntList);
    }

    public interface ArrayObject {
        @SqlQuery(U_SELECT)
        UUID[] fetchUuidArray();

        @SqlUpdate(U_INSERT)
        void insertUuidArray(UUID[] uuids);

        @SqlQuery(U_SELECT)
        List<UUID> fetchUuidList();

        @SqlUpdate(U_INSERT)
        void insertUuidList(List<UUID> u);


        @SqlQuery(I_SELECT)
        int[] fetchIntArray();

        @SqlQuery(I_SELECT)
        Integer[] fetchBoxedIntArray();

        @SqlQuery(I_SELECT)
        Object[] fetchObjectArray();

        @SqlUpdate(I_INSERT)
        void insertIntArray(int[] ints);

        @SqlUpdate(I_INSERT)
        void insertBoxedIntArray(Integer[] ints);

        @SqlQuery(I_SELECT)
        List<Integer> fetchIntList();

        @SqlUpdate(I_INSERT)
        void insertIntList(List<Integer> u);
    }
}
