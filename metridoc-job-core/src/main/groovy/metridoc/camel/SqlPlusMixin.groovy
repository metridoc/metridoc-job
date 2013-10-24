/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */


package metridoc.camel

import metridoc.sql.SqlPlus

import javax.sql.DataSource
import java.sql.Statement

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 8/5/11
 * Time: 9:50 AM
 */
class SqlPlusMixin {

    SqlPlus sql

    SqlPlus getSql() {
        if (sql) {
            return sql
        }

        sql = new SqlPlus(getDataSource())
        sql.withStatement {Statement statement ->
            statement.setFetchSize(getFetchSize())
        }

        return sql
    }

    DataSource getDataSource() {
        return endpoint.dataSource
    }

    int getFetchSize() {
        return endpoint.fetchSize
    }

    int getBatchSize() {
        return endpoint.batchSize
    }

    boolean getLogBatches() {
        return endpoint.logBatches
    }

    String getQuery() {
        return endpoint.query
    }
}
