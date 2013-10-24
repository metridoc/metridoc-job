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



package metridoc.sql

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import spock.lang.Specification

class SqlPlusSpec extends Specification {

    def dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build()

    def cleanup() {
        dataSource.shutdown()
    }

    def "test corner cases"() {
        given:
        def sql = new SqlPlus(dataSource)

        when: "running a batch insert with an empty list"
        sql.runBatch("foo", [])

        then: "then there will be no error"
        notThrown Throwable

        when: "running a batch insert with an empty hash"
        sql.runBatch("foo", [:])

        then: "then there will be no error"
        notThrown Throwable
    }
}
