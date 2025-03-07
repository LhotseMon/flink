/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.planner.plan.nodes.exec.stream;

import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.planner.utils.StreamTableTestUtil;
import org.apache.flink.table.planner.utils.TableTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test json serialization/deserialization for deduplicate. */
class DeduplicationJsonPlanTest extends TableTestBase {

    private StreamTableTestUtil util;
    private TableEnvironment tEnv;

    @BeforeEach
    void setup() {
        util = streamTestUtil(TableConfig.getDefault());
        tEnv = util.getTableEnv();
    }

    @Test
    void testDeduplication() {
        String srcTableDdl =
                "CREATE TABLE srcValuesTable (\n"
                        + "  order_id bigint,\n"
                        + "  `user` varchar,\n"
                        + "  product varchar,\n"
                        + "  order_time timestamp(3),\n"
                        + "  proctime AS PROCTIME()\n"
                        + ") with (\n"
                        + "  'connector' = 'values',\n"
                        + "  'bounded' = 'false',"
                        + "  'disable-lookup' = 'true')";
        tEnv.executeSql(srcTableDdl);
        String sinkTableDdl =
                "CREATE TABLE sink (\n"
                        + "  order_id bigint,\n"
                        + "  `user` varchar,\n"
                        + "  product varchar,\n"
                        + "  order_time timestamp(3)\n"
                        + ") with (\n"
                        + "  'connector' = 'values',\n"
                        + "  'table-sink-class' = 'DEFAULT')";
        tEnv.executeSql(sinkTableDdl);
        util.verifyJsonPlan(
                "insert into sink "
                        + "select order_id, user, product, order_time \n"
                        + "FROM (\n"
                        + "  SELECT *,\n"
                        + "    ROW_NUMBER() OVER (PARTITION BY product ORDER BY proctime ASC) AS row_num\n"
                        + "  FROM srcValuesTable)\n"
                        + "WHERE row_num = 1 \n");
    }
}
